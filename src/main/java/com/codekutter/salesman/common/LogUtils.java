/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Copyright (c) $year
 * Date: 1/1/19 11:14 AM
 * Subho Ghosh (subho dot ghosh at outlook.com)
 *
 */

package com.codekutter.salesman.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Utility functions for Logging.
 */
public class LogUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("DEFAULT");

    public static boolean isDebugEnabled() {
        return LOGGER.isDebugEnabled();
    }

    /**
     * Log an INFO message to the default logger handle.
     *
     * @param caller - Calling class.
     * @param mesg   - Message to log.
     * @param LOGGER - Logger handle to log to.
     * @param level  - Log level to write (default=DEBUG).
     */
    private static void LOG(Class<?> caller, String mesg, Logger LOGGER,
                            ELOGLELVEL level) {
        if (caller == null) {
            caller = LogUtils.class;
        }
        if (LOGGER == null) {
            LOGGER = LogUtils.LOGGER;
        }
        if (level == null) {
            level = ELOGLELVEL.DEBUG;
        }
        switch (level) {
            case INFO:
                LOGGER.info(
                        String.format("[%s]: %s", caller.getCanonicalName(), mesg));
                break;
            case WARN:
                LOGGER.warn(
                        String.format("[%s]: %s", caller.getCanonicalName(), mesg));
                break;
            case DEBUG:
                LOGGER.debug(
                        String.format("[%s]: %s", caller.getCanonicalName(), mesg));
                break;
            case ERROR:
                LOGGER.error(
                        String.format("[%s]: %s", caller.getCanonicalName(), mesg));
                break;
            case TRACE:
                LOGGER.trace(
                        String.format("[%s]: %s", caller.getCanonicalName(), mesg));
                break;
        }

    }

    /**
     * Write a serialized object instance to the log.
     *
     * @param caller - Calling class.
     * @param source - Object instance to serialize.
     * @param LOGGER - Logger handle to log to.
     * @param level  - Log level to write.
     */
    private static void WRITE(Class<?> caller, Object source, Logger LOGGER,
                              ELOGLELVEL level) {
        try {
            String mesg = "";
            if (source != null) {
                ObjectMapper mapper = Constants.getJsonMapper();
                mesg = mapper.writeValueAsString(source);
            }
            LOG(caller, mesg, LOGGER, level);
        } catch (Exception e) {
            LOG(LogUtils.class, e.getLocalizedMessage(), null, ELOGLELVEL.WARN);
        }
    }

    /**
     * Log an error message.
     *
     * @param caller - Calling class.
     * @param error  - Error handle to log.
     * @param LOGGER - Logger handle to log to.
     * @param level  - Log level to write (default=ERROR).
     */
    private static void ERROR(Class<?> caller, Throwable error, Logger LOGGER,
                              ELOGLELVEL level) {
        if (level == null) {
            level = ELOGLELVEL.ERROR;
        }
        String mesg = error.getLocalizedMessage();
        LOG(caller, mesg, LOGGER, level);
    }

    /**
     * Log the stacktrace for the given exception. Will recursively Log exception stacktrace
     * in case there is nested exceptions.
     * <p>
     * By default it will use a DEBUG logging level.
     *
     * @param caller - Calling class.
     * @param error  - Error handle to log stacktrace for.
     * @param LOGGER - Logger handle to log to.
     * @param level  - Log level to write (default=DEBUG).
     */
    private static void STACKTRACE(Class<?> caller, Throwable error, Logger LOGGER,
                                   ELOGLELVEL level) {
        if (error == null) {
            return;
        }
        if (!LogUtils.LOGGER.isDebugEnabled()) {
            return;
        }
        if (level == null) {
            level = ELOGLELVEL.DEBUG;
        }
        String mesg = String.format("\n%s[START:%s]%s", StringUtils.repeat("*", 32),
                error.getClass().getCanonicalName(),
                StringUtils.repeat("*", 32));
        LOG(caller, mesg, LOGGER, level);
        ERROR(caller, error, LOGGER, level);


        LOG(caller, getStackTrace(error), LOGGER, level);

        mesg = String.format("\n%s[END  :%s]%s", StringUtils.repeat("*", 32),
                error.getClass().getCanonicalName(),
                StringUtils.repeat("*", 32));
        LOG(caller, mesg, LOGGER, level);
        if (error.getClass() != null) {
            STACKTRACE(caller, error.getCause(), LOGGER, level);
        }
    }

    public static String getStackTrace(@Nonnull Throwable error) {
        StringBuffer buffer = new StringBuffer();

        StackTraceElement[] stack = error.getStackTrace();
        if (stack != null && stack.length > 0) {
            for (StackTraceElement se : stack) {
                buffer.append(se.toString()).append("\n");
            }
        }
        return buffer.toString();
    }

    /**
     * Log an error message.
     *
     * @param caller - Calling class
     * @param mesg   - Error message to Log
     */
    public static void error(Class<?> caller, String mesg) {
        LOG(caller, mesg, null, ELOGLELVEL.ERROR);
    }

    /**
     * Log an error message to the specified Log handle.
     *
     * @param caller - Calling class
     * @param mesg   - Error message to Log
     * @param LOG    - Log handle to log to.
     */
    public static void error(Class<?> caller, String mesg, Logger LOG) {
        LOG(caller, mesg, LOG, ELOGLELVEL.ERROR);
    }

    /**
     * Log the exception message.
     *
     * @param caller - Calling class
     * @param error  - Exception to Log.
     */
    public static void error(Class<?> caller, Throwable error) {
        ERROR(caller, error, null, null);
        STACKTRACE(caller, error, null, ELOGLELVEL.DEBUG);
    }

    /**
     * Log the exception message to the specified Log handle.
     *
     * @param caller - Calling class
     * @param error  - Exception to Log.
     * @param LOG    - Log handle to log to.
     */
    public static void error(Class<?> caller, Throwable error, Logger LOG) {
        ERROR(caller, error, LOG, null);
        STACKTRACE(caller, error, LOG, ELOGLELVEL.DEBUG);
    }

    /**
     * Log an warning message.
     *
     * @param caller - Calling class
     * @param mesg   - Error message to Log
     */
    public static void warn(Class<?> caller, String mesg) {
        LOG(caller, mesg, null, ELOGLELVEL.WARN);
    }

    /**
     * Log an warning message to the specified Log handle.
     *
     * @param caller - Calling class
     * @param mesg   - Error message to Log
     * @param LOG    - Log handle to log to.
     */
    public static void warn(Class<?> caller, String mesg, Logger LOG) {
        LOG(caller, mesg, LOG, ELOGLELVEL.WARN);
    }

    /**
     * Log the exception message as a warning.
     *
     * @param caller - Calling class
     * @param error  - Exception to Log.
     */
    public static void warn(Class<?> caller, Throwable error) {
        ERROR(caller, error, null, ELOGLELVEL.WARN);
        STACKTRACE(caller, error, null, ELOGLELVEL.DEBUG);
    }

    /**
     * Log the exception message as a warning to the specified Log handle.
     *
     * @param caller - Calling class
     * @param error  - Exception to Log.
     * @param LOG    - Log handle to log to.
     */
    public static void warn(Class<?> caller, Throwable error, Logger LOG) {
        ERROR(caller, error, LOG, ELOGLELVEL.WARN);
        STACKTRACE(caller, error, LOG, ELOGLELVEL.DEBUG);
    }

    /**
     * Log an info message.
     *
     * @param caller - Calling class
     * @param mesg   - Error message to Log
     */
    public static void info(Class<?> caller, String mesg) {
        LOG(caller, mesg, null, ELOGLELVEL.INFO);
    }

    /**
     * Log an info message to the specified Log handle.
     *
     * @param caller - Calling class
     * @param mesg   - Error message to Log
     * @param LOG    - Log handle to log to.
     */
    public static void info(Class<?> caller, String mesg, Logger LOG) {
        LOG(caller, mesg, LOG, ELOGLELVEL.INFO);
    }

    /**
     * Log an debug message.
     *
     * @param caller - Calling class
     * @param mesg   - Error message to Log
     */
    public static void debug(Class<?> caller, String mesg) {
        LOG(caller, mesg, null, ELOGLELVEL.DEBUG);
    }

    /**
     * Log an debug message to the specified Log handle.
     *
     * @param caller - Calling class
     * @param mesg   - Error message to Log
     * @param LOG    - Log handle to log to.
     */
    public static void debug(Class<?> caller, String mesg, Logger LOG) {
        LOG(caller, mesg, LOG, ELOGLELVEL.DEBUG);
    }

    /**
     * Log the object source as a debug message.
     *
     * @param caller - Calling class
     * @param source - Object source to log.
     */
    public static void debug(Class<?> caller, Object source) {
        WRITE(caller, source, null, ELOGLELVEL.DEBUG);
    }

    /**
     * Log the object source as a debug message to the specified Log handle.
     *
     * @param caller - Calling class
     * @param source - Object source to log.
     * @param LOG    - Log handle to log to.
     */
    public static void debug(Class<?> caller, Object source, Logger LOG) {
        WRITE(caller, source, LOG, ELOGLELVEL.DEBUG);
    }

    /**
     * Enum defining the logging levels.
     */
    public static enum ELOGLELVEL {
        /**
         * Information log
         */
        INFO,
        /**
         * Warning Log
         */
        WARN,
        /**
         * Error Log
         */
        ERROR,
        /**
         * Debug Log
         */
        DEBUG,
        /**
         * Trace Log
         */
        TRACE
    }
}
