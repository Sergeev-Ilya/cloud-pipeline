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

package com.epam.pipeline.manager.pipeline.runner;

import com.epam.pipeline.entity.configuration.PipeConfValueVO;
import com.epam.pipeline.entity.pipeline.run.OsType;
import com.epam.pipeline.entity.pipeline.run.PipeRunCmdStartVO;
import com.epam.pipeline.entity.pipeline.run.PipelineStart;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class builds 'pipe run' CLI command according to specified input arguments {@link PipeRunCmdStartVO}
 */
public class PipeRunCmdBuilder {

    private static final String WHITESPACE = " ";
    private static final String CMD_OPTIONS_DELIMITER = "--";

    private final PipelineStart runVO;
    private final PipeRunCmdStartVO startVO;
    private final List<String> cmd;
    private final OsType runCmdExecutionEnvironment;

    public PipeRunCmdBuilder(final PipeRunCmdStartVO startVO) {
        this.startVO = startVO;
        this.runVO = startVO.getPipelineStart();
        this.cmd = new ArrayList<>();
        this.cmd.add("pipe run");
        this.runCmdExecutionEnvironment = startVO.getRunStartCmdExecutionEnvironment();
    }

    public String build() {
        return String.join(WHITESPACE, cmd);
    }

    public PipeRunCmdBuilder name() {
        if (Objects.isNull(runVO.getPipelineId())) {
            return this;
        }
        if (StringUtils.isNotBlank(runVO.getVersion())) {
            addCmd(String.format("-n %d@%s", runVO.getPipelineId(), runVO.getVersion()));
        } else {
            addCmd(String.format("-n %d", runVO.getPipelineId()));
        }
        return this;
    }

    public PipeRunCmdBuilder instanceDisk() {
        buildObjectCmdArg("-id", runVO.getHddSize());
        return this;
    }

    public PipeRunCmdBuilder instanceType() {
        buildStringCmdArg("-it", runVO.getInstanceType());
        return this;
    }

    public PipeRunCmdBuilder dockerImage() {
        buildStringCmdArg("-di", runVO.getDockerImage());
        return this;
    }

    public PipeRunCmdBuilder cmdTemplate() {
        if (StringUtils.isNotBlank(runVO.getCmdTemplate())) {
            cmd.add(getNewLineIndicator());
            cmd.add("-cmd");
            cmd.add(quoteArgumentValue(runVO.getCmdTemplate()));
        }
        return this;
    }

    public PipeRunCmdBuilder timeout() {
        buildObjectCmdArg("-t", runVO.getTimeout());
        return this;
    }

    public PipeRunCmdBuilder instanceCount() {
        final Integer nodeCount = runVO.getNodeCount();
        if (Objects.nonNull(nodeCount) && nodeCount == 0) {
            return this;
        }
        buildObjectCmdArg("-ic", nodeCount);
        return this;
    }

    public PipeRunCmdBuilder priceType() {
        cmd.add(getNewLineIndicator());
        cmd.add("-pt");
        cmd.add(isOnDemand() ? "on-demand" : "spot");
        return this;
    }

    public PipeRunCmdBuilder regionId() {
        buildObjectCmdArg("-r", runVO.getCloudRegionId());
        return this;
    }

    public PipeRunCmdBuilder parentNode() {
        buildObjectCmdArg("-pn", runVO.getParentNodeId());
        return this;
    }

    public PipeRunCmdBuilder config() {
        buildStringCmdArg("-c", runVO.getConfigurationName());
        return this;
    }

    public PipeRunCmdBuilder yes() {
        if (startVO.isYes()) {
            addCmd("-y");
        }
        return this;
    }

    public PipeRunCmdBuilder quite() {
        if (startVO.isQuite()) {
            addCmd("-q");
        }
        return this;
    }

    public PipeRunCmdBuilder sync() {
        if (startVO.isSync()) {
            addCmd("-s");
        }
        return this;
    }

    public PipeRunCmdBuilder parameters() {
        if (startVO.isShowParams()) {
            addCmd("-p");
        }
        return this;
    }

    public PipeRunCmdBuilder runParameters() {
        if (MapUtils.isNotEmpty(runVO.getParams())) {
            addCmd(CMD_OPTIONS_DELIMITER);
            final String parametersCommand = runVO.getParams().entrySet()
                    .stream()
                    .map(this::prepareParams)
                    .collect(Collectors.joining(WHITESPACE + getNewLineIndicator() + WHITESPACE));
            cmd.add(parametersCommand);
        }
        if (Objects.nonNull(runVO.getParentRunId())) {
            cmd.add(getNewLineIndicator());
            if (MapUtils.isEmpty(runVO.getParams())) {
                cmd.add(CMD_OPTIONS_DELIMITER);
            }
            cmd.add(String.format("parent-id %d", runVO.getParentRunId()));
        }
        return this;
    }

    public PipeRunCmdBuilder nonPause() {
        if (isOnDemand() && runVO.isNonPause()) {
            addCmd("-np");
        }
        return this;
    }

    private String prepareParams(final Map.Entry<String, PipeConfValueVO> entry) {
        return entry.getKey() + WHITESPACE + quoteArgumentValue(getValueWithType(entry.getValue()));
    }

    private String getValueWithType(final PipeConfValueVO entry) {
        final String value = entry.getValue();
        final String type = entry.getType();
        if (StringUtils.isBlank(value) || StringUtils.isBlank(type) ||
                PipeConfValueVO.DEFAULT_TYPE.equals(type)) {
            return value;
        }
        return type + "?" + value;
    }

    private void buildObjectCmdArg(final String argumentName, final Object argumentValue) {
        if (Objects.nonNull(argumentValue)) {
            cmd.add(getNewLineIndicator());
            cmd.add(argumentName);
            cmd.add(String.valueOf(argumentValue));
        }
    }

    private void buildStringCmdArg(final String argumentName, final String argumentValue) {
        if (StringUtils.isNotBlank(argumentValue)) {
            cmd.add(getNewLineIndicator());
            cmd.add(argumentName);
            cmd.add(argumentValue);
        }
    }

    private String quoteArgumentValue(final String value) {
        final Character quote = getQuotes();
        return String.format("%c%s%c", quote, escapeDoubleQuotes(value), quote);
    }

    private Character getQuotes() {
        switch (runCmdExecutionEnvironment) {
            case WINDOWS:
                return '"';
            default:
                return '\'';
        }
    }

    private String escapeDoubleQuotes(final String value) {
        return OsType.WINDOWS.equals(runCmdExecutionEnvironment)
               ? value.replaceAll("\"", "\\\\\"")
               : value;
    }

    private boolean isOnDemand() {
        return Objects.nonNull(runVO.getIsSpot()) && !runVO.getIsSpot();
    }

    private void addCmd(final String value) {
        cmd.add(getNewLineIndicator());
        cmd.add(value);
    }

    private String getNewLineIndicator() {
        return OsType.WINDOWS.equals(runCmdExecutionEnvironment)
                ? "^\n"
                : "\\\n";
    }
}
