/*
 * Copyright (C) 2020 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.hardware.face.sense;

/** @hide */
interface ISenseService {
    int authenticate(in byte[] token, int width, int height);

    oneway void cancelEnrollment();

    oneway void endAuthenticate();

    oneway void endEnrollment();

    int enroll(in byte[] token, int width, int height, int left, int right, int top, int bottom);

    int getActiveUserCount();

    int getEnrollmentSteps();

    String getSdkType();

    oneway void removeActiveUser(long userId);

    oneway void setActiveUser(long userId);
}
