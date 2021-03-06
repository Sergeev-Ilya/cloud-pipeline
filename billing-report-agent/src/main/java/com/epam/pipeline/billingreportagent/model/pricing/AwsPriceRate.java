/*
 * Copyright 2017-2020 EPAM Systems, Inc. (https://www.epam.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.pipeline.billingreportagent.model.pricing;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;
import java.util.Map;

@Data
public class AwsPriceRate {

    private String unit;

    private String rateCode;

    private Long beginRange;

    private Long endRange;

    private Map<String, Double> pricePerUnit;

    private String description;

    private List<String> appliesTo;

    @JsonSetter("endRange")
    public void setEndRange(String endRange) {
        if (NumberUtils.isCreatable(endRange)) {
            this.endRange = Long.parseLong(endRange);
        } else {
            this.endRange = Long.MAX_VALUE;
        }
    }
}
