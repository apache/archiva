package org.apache.archiva.repository;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import java.time.*;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A definition of schedule times.
 */
public class ScheduleDefinition {


    final SortedSet<DayOfWeek> daysOfWeek = new TreeSet<>();

    final SortedSet<MonthDay> daysOfMonth = new TreeSet<>();

    final SortedSet<LocalTime> scheduleTimes = new TreeSet<>();

    final LocalTime startTime;

    final Duration timeInterval;

    boolean fixedTimes = false;


    public ScheduleDefinition(Collection<DayOfWeek> daysOfWeek,
                              Collection<MonthDay> daysOfMonth,
                              Collection<LocalTime> scheduleTimes,
                              LocalTime startTime, Duration timeInterval) {
        if (daysOfWeek!=null)
        this.daysOfWeek.addAll(daysOfWeek);
        if (daysOfMonth!=null)
        this.daysOfMonth.addAll(daysOfMonth);
        if (scheduleTimes!=null)
        this.scheduleTimes.addAll(scheduleTimes);
        this.startTime = startTime;
        this.timeInterval = timeInterval;
    }

    /**
     * Returns the days of the week on which the action should be run.
     * @return The set of week days.
     */
    public SortedSet<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    /**
     * Returns the days in the month on which the action should be run.
     * @return The set of days.
     */
    public SortedSet<MonthDay> getDaysOfMonth() {
        return daysOfMonth;
    }

    /**
     * Returns the time on each day on which the action should be run.
     * @return a set of times on which the action should be run.
     */
    public SortedSet<LocalTime> getScheduleTimes() {
        return scheduleTimes;
    }

    /**
     * Returns the start time each day on which the action should be run.
     * @return the start time.
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * The interval after that the next run should be scheduled.
     * @return The interval between runs.
     */
    public Duration getTimeInterval() {
        return timeInterval;
    }

    /**
     * Returns true, if the task should be run on fixed times. Otherwise
     * the tasks are scheduled repeatedly with the time interval.
     * @return true, if the schedule times are fixed.
     */
    public boolean isFixedTimes() {
        return fixedTimes;
    };
}
