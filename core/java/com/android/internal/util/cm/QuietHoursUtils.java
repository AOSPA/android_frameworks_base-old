/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.cm;

import android.content.Context;
import android.provider.Settings;

import java.util.Calendar;

public class QuietHoursUtils {
    public static boolean inQuietHours(Context context, String option) {
        boolean quietHoursEnabled = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QUIET_HOURS_ENABLED, 0) != 0;
        int quietHoursStart = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QUIET_HOURS_START, 0);
        int quietHoursEnd = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QUIET_HOURS_END, 0);
        boolean quietHoursOption = Settings.System.getInt(context.getContentResolver(),
                option, 0) != 0;
        if (quietHoursEnabled && quietHoursOption && (quietHoursStart != quietHoursEnd)) {
                // Get the date in "quiet hours" format.
                final Calendar calendar = Calendar.getInstance();
                final int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + (calendar.get(Calendar.MINUTE) + 1);
                final int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                switch (dayOfWeek) {
                    case Calendar.MONDAY:
                        boolean quietHoursMonday = Settings.System.getInt(context.getContentResolver(),
                                Settings.System.QUIET_HOURS_MONDAY, 0) != 0;
                        if (!quietHoursMonday) {
                            return false;
                        }
                        break;
                    case Calendar.TUESDAY:
                        boolean quietHoursTuesday = Settings.System.getInt(context.getContentResolver(),
                                Settings.System.QUIET_HOURS_TUESDAY, 0) != 0;
                        if (!quietHoursTuesday) {
                            return false;
                        }
                        break;
                    case Calendar.WEDNESDAY:
                        boolean quietHoursWednesday = Settings.System.getInt(context.getContentResolver(),
                                Settings.System.QUIET_HOURS_WEDNESDAY, 0) != 0;
                        if (!quietHoursWednesday) {
                            return false;
                        }
                        break;
                    case Calendar.THURSDAY:
                        boolean quietHoursThursday = Settings.System.getInt(context.getContentResolver(),
                                Settings.System.QUIET_HOURS_THURSDAY, 0) != 0;
                        if (!quietHoursThursday) {
                            return false;
                        }
                        break;
                    case Calendar.FRIDAY:
                        boolean quietHoursFriday = Settings.System.getInt(context.getContentResolver(),
                                Settings.System.QUIET_HOURS_FRIDAY, 0) != 0;
                        if (!quietHoursFriday) {
                            return false;
                        }
                        break;
                    case Calendar.SATURDAY:
                        boolean quietHoursSaturday = Settings.System.getInt(context.getContentResolver(),
                                Settings.System.QUIET_HOURS_SATURDAY, 0) != 0;
                        if (!quietHoursSaturday) {
                            return false;
                        }
                        break;
                    case Calendar.SUNDAY:
                        boolean quietHoursSunday = Settings.System.getInt(context.getContentResolver(),
                                Settings.System.QUIET_HOURS_SUNDAY, 0) != 0;
                        if (!quietHoursSunday) {
                            return false;
                        }
                        break;
                    default:
                        break;
                }
                if (quietHoursEnd < quietHoursStart) {
                    // Starts at night, ends in the morning.
                    return (minutes > quietHoursStart) || (minutes < quietHoursEnd);
                } else {
                    return (minutes > quietHoursStart) && (minutes < quietHoursEnd);
                }
        }
        return false;
    }
}
