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

namespace {

// Android reads tags through musikr's JMetadataBuilder, which builds Android metadata objects.
// The desktop port has no such objects, so the same taglib calls are surfaced as a plain array.
// taglib 2.x trims Tag to title/artist/album/track/year; album artist and disc only live on the
// format-specific tag classes, so they are not read yet.
constexpr int FIELD_COUNT = 6;

std::string toUtf8(const TagLib::String &value) {
    return value.isEmpty() ? std::string() : value.to8Bit(true);
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
        }

        if (const TagLib::AudioProperties *properties = file.audioProperties()) {
            if (properties->lengthInSeconds() > 0) {
                fields[5] = std::to_string(properties->lengthInSeconds());
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
