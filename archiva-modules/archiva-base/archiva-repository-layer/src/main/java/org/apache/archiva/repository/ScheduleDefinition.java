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


import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.expression.Always;
import com.cronutils.model.field.expression.And;
import com.cronutils.model.field.expression.Between;
import com.cronutils.model.field.expression.Every;
import com.cronutils.model.field.expression.FieldExpression;
import com.cronutils.model.field.expression.On;
import com.cronutils.model.field.expression.QuestionMark;
import com.cronutils.model.field.expression.visitor.FieldExpressionVisitor;
import com.cronutils.model.field.value.IntegerFieldValue;
import com.cronutils.parser.CronParser;
import org.apache.tools.ant.types.resources.Sort;

import java.time.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.cronutils.model.CronType.QUARTZ;

/**
 * A definition of schedule times.
 */
public class ScheduleDefinition {


    final SortedSet<DayOfWeek> daysOfWeek = new TreeSet<>();

    final SortedSet<MonthDay> daysOfMonth = new TreeSet<>();

    final SortedSet<Month> months = new TreeSet<>(  );

    final SortedSet<LocalTime> scheduleTimes = new TreeSet<>();

    final LocalTime startTime;

    final Duration timeInterval;

    boolean fixedTimes = false;

    public ScheduleDefinition(Collection<DayOfWeek> daysOfWeek,
                              Collection<MonthDay> daysOfMonth,
                              Collection<Month> months,
                              Collection<LocalTime> scheduleTimes,
                              LocalTime startTime, Duration timeInterval) {
        if (daysOfWeek!=null)
        this.daysOfWeek.addAll(daysOfWeek);
        if (daysOfMonth!=null)
        this.daysOfMonth.addAll(daysOfMonth);
        if (months!=null) {
            this.months.addAll(months);
        }
        if (scheduleTimes!=null && scheduleTimes.size()>0)
        {
            this.fixedTimes = true;
            this.scheduleTimes.addAll( scheduleTimes );
            this.startTime=null;
            this.timeInterval=null;
        } else
        {
            this.fixedTimes = false;
            this.startTime = startTime;
            this.timeInterval = timeInterval;
        }
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
     * Returns the months on which the action should be run.
     * @return
     */
    public SortedSet<Month> getMonths()  {
        return months;
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

    public static ScheduleDefinition fromCronExpression(String cron) {
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor( QUARTZ );
        CronParser parser = new CronParser( cronDefinition  );
        Cron pCron = parser.parse(cron);
        if (pCron.validate()==null) {
            throw new IllegalArgumentException( "Cron expression not valid "+cron );
        };
        CronVisitor secondsVisit = new CronVisitor( );
        pCron.retrieve( CronFieldName.SECOND ).getExpression().accept(secondsVisit);
        CronVisitor minutesVisit = new CronVisitor( );
        pCron.retrieve( CronFieldName.MINUTE ).getExpression().accept(minutesVisit);
        CronVisitor hoursVisit = new CronVisitor( 24 );
        pCron.retrieve( CronFieldName.HOUR ).getExpression().accept(hoursVisit);
        SortedSet<LocalTime> times = new TreeSet<>(  );
        for (Integer hour : hoursVisit.getTimes()) {
            for (Integer minute : minutesVisit.getTimes()) {
                for (Integer second : secondsVisit.getTimes()) {
                    times.add(LocalTime.of( hour, minute, second));
                }
            }
        }

        return null;
    }

    private static class CronVisitor implements FieldExpressionVisitor {

        private int range = 60;
        private SortedSet<Integer> times = new TreeSet<>(  );

        CronVisitor() {

        }

        CronVisitor(int range) {
            this.range = range;
        }

        private SortedSet<Integer> getTimes() {
            return times;
        }

        @Override
        public FieldExpression visit( FieldExpression expression )
        {
            try {
                Integer in = new Integer(expression.asString());
                times.add(in);
            } catch (NumberFormatException ex) {
                //
            }
            return expression;
        }

        @Override
        public FieldExpression visit( Always always )
        {
            for (int i=0; i<range; i++) {
                times.add(new Integer(i));
            }
            return always;
        }

        @Override
        public FieldExpression visit( And and )
        {
            FieldExpression result = null;
            for (FieldExpression expr : and.getExpressions()) {
                result = expr.accept( this );
            }
            return result;
        }

        @Override
        public FieldExpression visit( Between between )
        {
            for (int i=((IntegerFieldValue) between.getFrom( ).getValue( )).getValue();
                 i<((IntegerFieldValue)between.getTo().getValue()).getValue() && i<range; i++ ){
                times.add(new Integer(i));
            }
            return between;
        }

        @Override
        public FieldExpression visit( Every every )
        {
            String exp = every.getExpression().asString();
            int start;
            if ("*".equals(exp)) {
                start = 0;
            } else {
                start = Integer.parseInt( exp );
            }
            int period = every.getPeriod().getValue();
            for (int i=start; i<range; i=i+period) {
                times.add(new Integer(i));
            }
            return every;
        }

        @Override
        public FieldExpression visit( On on )
        {
            // Ignore
            return null;
        }

        @Override
        public FieldExpression visit( QuestionMark questionMark )
        {
            // Ignore
            return null;
        }
    }
}
