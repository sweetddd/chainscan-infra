/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.everylink.chainscan.watcher.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Date util
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-26
 */
public final class DateUtil {

    private DateUtil(){}

    /**
     * format as 'yyyy-MM-dd HH:mm:ss'
     * @param d
     * @return
     */
    public static String format_yyyy_MM_dd_HH_mm_ss(Date d) {
        if (d == null) {
            return null;
        }

        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }
}
