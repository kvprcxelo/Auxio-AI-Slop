/*
 * Copyright (c) 2026 Auxio Project
 * tagreader.cpp is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#include <jni.h>
#include <string>
#include <vector>

#include "taglib/audioproperties.h"
#include "taglib/fileref.h"
#include "taglib/tag.h"
#include "taglib/tbytevector.h"
#include "taglib/tpropertymap.h"

// Format-specific classes, reached only for embedded artwork: taglib keeps pictures off the base
// Tag interface, so each container has to be asked in its own way.
#include "taglib/attachedpictureframe.h"
#include "taglib/flacfile.h"
#include "taglib/flacpicture.h"
#include "taglib/id3v2tag.h"
#include "taglib/mp4coverart.h"
#include "taglib/mp4file.h"
#include "taglib/mp4item.h"
#include "taglib/mp4tag.h"
#include "taglib/mpegfile.h"
#include "taglib/opusfile.h"
#include "taglib/vorbisfile.h"
#include "taglib/xiphcomment.h"

namespace {

// Android reads tags through musikr's JMetadataBuilder, which builds Android metadata objects.
// The desktop port has no such objects, so the same taglib calls are surfaced as a plain array.
// taglib 2.x trims Tag to title/artist/album/track/year; album artist and disc only live on the
// format-specific tag classes, so they are not read yet.
//
// Fields: title, artist, album, track, year, date, genre, duration.
constexpr int FIELD_COUNT = 8;

std::string toUtf8(const TagLib::String &value) {
    return value.isEmpty() ? std::string() : value.to8Bit(true);
}

TagLib::ByteVector xiphPicture(TagLib::Ogg::XiphComment *comment) {
    if (comment == nullptr) {
        return {};
    }

    const auto &pictures = comment->pictureList();
    TagLib::ByteVector fallback;
    for (const auto *picture : pictures) {
        if (picture->type() == TagLib::FLAC::Picture::FrontCover) {
            return picture->data();
        }
        if (fallback.isEmpty()) {
            fallback = picture->data();
        }
    }
    return fallback;
}

/** The front cover of a file, or the first picture it has, or nothing. */
TagLib::ByteVector embeddedPicture(TagLib::File *file) {
    if (file == nullptr) {
        return {};
    }

    if (auto *mpeg = dynamic_cast<TagLib::MPEG::File *>(file)) {
        TagLib::ID3v2::Tag *tag = mpeg->ID3v2Tag();
        if (tag == nullptr) {
            return {};
        }
        TagLib::ByteVector fallback;
        for (auto *frame : tag->frameList("APIC")) {
            auto *picture = dynamic_cast<TagLib::ID3v2::AttachedPictureFrame *>(frame);
            if (picture == nullptr) {
                continue;
            }
            if (picture->type() == TagLib::ID3v2::AttachedPictureFrame::FrontCover) {
                return picture->picture();
            }
            if (fallback.isEmpty()) {
                fallback = picture->picture();
            }
        }
        return fallback;
    }

    if (auto *flac = dynamic_cast<TagLib::FLAC::File *>(file)) {
        TagLib::ByteVector fallback;
        for (const auto *picture : flac->pictureList()) {
            if (picture->type() == TagLib::FLAC::Picture::FrontCover) {
                return picture->data();
            }
            if (fallback.isEmpty()) {
                fallback = picture->data();
            }
        }
        return fallback;
    }

    if (auto *opus = dynamic_cast<TagLib::Ogg::Opus::File *>(file)) {
        return xiphPicture(opus->tag());
    }

    if (auto *vorbis = dynamic_cast<TagLib::Ogg::Vorbis::File *>(file)) {
        return xiphPicture(vorbis->tag());
    }

    if (auto *mp4 = dynamic_cast<TagLib::MP4::File *>(file)) {
        TagLib::MP4::Tag *tag = mp4->tag();
        if (tag == nullptr) {
            return {};
        }
        const TagLib::MP4::Item item = tag->item("covr");
        if (!item.isValid()) {
            return {};
        }
        const TagLib::MP4::CoverArtList covers = item.toCoverArtList();
        return covers.isEmpty() ? TagLib::ByteVector() : covers.front().data();
    }

    return {};
}

} // namespace

extern "C" JNIEXPORT jobjectArray JNICALL
Java_org_oxycblt_auxio_linux_music_NativeTags_readNative(JNIEnv *env, jclass, jstring jPath) {
    if (jPath == nullptr) {
        return nullptr;
    }

    const char *pathChars = env->GetStringUTFChars(jPath, nullptr);
    if (pathChars == nullptr) {
        return nullptr;
    }
    const std::string path(pathChars);
    env->ReleaseStringUTFChars(jPath, pathChars);

    std::vector<std::string> fields(FIELD_COUNT);

    try {
        TagLib::FileRef file(path.c_str(), true, TagLib::AudioProperties::Accurate);
        if (file.isNull()) {
            return nullptr;
        }

        if (const TagLib::Tag *tag = file.tag()) {
            fields[0] = toUtf8(tag->title());
            fields[1] = toUtf8(tag->artist());
            fields[2] = toUtf8(tag->album());
            if (tag->track() > 0) {
                fields[3] = std::to_string(tag->track());
            }
            if (tag->year() > 0) {
                fields[4] = std::to_string(tag->year());
            }
            fields[6] = toUtf8(tag->genre());
        }

        // The raw date tag keeps month/day where the format has them; Tag::year() only has the
        // year, which loses information the settings can format.
        if (TagLib::File *raw = file.file()) {
            const TagLib::PropertyMap map = raw->properties();
            const TagLib::StringList dates = map["DATE"];
            const TagLib::StringList years = map["YEAR"];
            if (!dates.isEmpty()) {
                fields[5] = toUtf8(dates.front());
            } else if (!years.isEmpty()) {
                fields[5] = toUtf8(years.front());
            }
        }

        if (const TagLib::AudioProperties *properties = file.audioProperties()) {
            if (properties->lengthInSeconds() > 0) {
                fields[7] = std::to_string(properties->lengthInSeconds());
            }
        }
    } catch (...) {
        // Unreadable or malformed files are simply untagged.
        return nullptr;
    }

    jclass stringClass = env->FindClass("java/lang/String");
    if (stringClass == nullptr) {
        return nullptr;
    }

    jobjectArray result = env->NewObjectArray(FIELD_COUNT, stringClass, nullptr);
    if (result == nullptr) {
        return nullptr;
    }

    for (int i = 0; i < FIELD_COUNT; ++i) {
        if (fields[i].empty()) {
            continue;
        }
        jstring value = env->NewStringUTF(fields[i].c_str());
        if (value == nullptr) {
            continue;
        }
        env->SetObjectArrayElement(result, i, value);
        env->DeleteLocalRef(value);
    }

    return result;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_oxycblt_auxio_linux_music_NativeTags_readArtworkNative(JNIEnv *env, jclass, jstring jPath) {
    if (jPath == nullptr) {
        return nullptr;
    }

    const char *pathChars = env->GetStringUTFChars(jPath, nullptr);
    if (pathChars == nullptr) {
        return nullptr;
    }
    const std::string path(pathChars);
    env->ReleaseStringUTFChars(jPath, pathChars);

    try {
        // Audio properties are irrelevant here, so skip parsing them.
        TagLib::FileRef file(path.c_str(), false);
        if (file.isNull()) {
            return nullptr;
        }

        const TagLib::ByteVector data = embeddedPicture(file.file());
        if (data.isEmpty()) {
            return nullptr;
        }

        const auto size = static_cast<jsize>(data.size());
        jbyteArray result = env->NewByteArray(size);
        if (result == nullptr) {
            return nullptr;
        }
        env->SetByteArrayRegion(result, 0, size, reinterpret_cast<const jbyte *>(data.data()));
        return result;
    } catch (...) {
        return nullptr;
    }
}
