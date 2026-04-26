package com.boboboom.jxc.workflow.application.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 流程 BPMN 模型支持类，负责将页面节点和连线配置转换为 Flowable 可执行定义。
 */
final class WorkflowBpmnModelSupport {

    static final String NODE_TYPE_NORMAL = "NORMAL";
    static final String NODE_TYPE_CONDITION = "CONDITION";
    static final String NODE_TYPE_SUCCESS = "SUCCESS";
    static final String NODE_TYPE_FAIL = "FAIL";
    static final String NODE_TYPE_START = "START";
    static final String NODE_TYPE_END = "END";
    static final String CONDITION_TRUE_EXPRESSION = "$.result==true";
    static final String CONDITION_FALSE_EXPRESSION = "$.result==false";

    private static final String BPMN_START_EVENT_ID = "start_event";
    private static final String BPMN_END_EVENT_ID = "end_event";
    private static final String BPMN_TASK_PREFIX = "task_";
    private static final String CONDITION_RESULT_VARIABLE = "result";
    private static final int START_NODE_X = 88;
    private static final int DEFAULT_TOP_Y = 76;
    private static final int DATA_FILL_X = 302;
    private static final int FINANCE_REVIEW_X = 514;
    private static final int SUCCESS_NODE_X = 764;
    private static final int SUCCESS_NODE_Y = 100;
    private static final int FAIL_NODE_X = 521;
    private static final int FAIL_NODE_Y = 280;
    private static final int END_NODE_X = 978;

    private WorkflowBpmnModelSupport() {
    }

    /**
     * 解析节点配置 JSON。
     *
     * @param configJson   节点 JSON
     * @param objectMapper JSON 组件
     * @return 节点配置
     */
    static List<NodeConfig> parseNodes(String configJson, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(configJson)) {
            return List.of();
        }
        try {
            List<NodeConfig> nodes = objectMapper.readValue(configJson, new TypeReference<>() {
            });
            return nodes == null ? List.of() : nodes;
        } catch (Exception ex) {
            throw new BusinessException("流程节点配置数据损坏");
        }
    }

    /**
     * 序列化节点配置。
     *
     * @param nodes        节点配置
     * @param objectMapper JSON 组件
     * @return 节点 JSON
     */
    static String toConfigJson(List<NodeConfig> nodes, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(nodes);
        } catch (Exception ex) {
            throw new BusinessException("流程配置序列化失败");
        }
    }

    /**
     * 标准默认流程节点配置 JSON。
     *
     * @return 默认流程 JSON
     */
    static String defaultNodeConfigJson() {
        List<NodeConfig> nodes = List.of(
                new NodeConfig("start_node", "开始", START_NODE_X, DEFAULT_TOP_Y, "", "OR", null, false, false,
                        NODE_TYPE_START, edgeJson(List.of(new EdgeLink("node_2", ""))), List.of()),
                new NodeConfig("node_2", "数据填报", DATA_FILL_X, DEFAULT_TOP_Y - 1, "SALESMAN", "OR", null, false, false,
                        NODE_TYPE_NORMAL, edgeJson(List.of(new EdgeLink("node_3", ""))), List.of()),
                new NodeConfig("node_3", "财务审核", FINANCE_REVIEW_X, DEFAULT_TOP_Y - 1, "FINANCE", "OR", null, false, false,
                        NODE_TYPE_CONDITION, edgeJson(List.of(
                                new EdgeLink("node_4", CONDITION_TRUE_EXPRESSION),
                                new EdgeLink("node_5", CONDITION_FALSE_EXPRESSION)
                        )), List.of()),
                new NodeConfig("node_4", "成功", SUCCESS_NODE_X, SUCCESS_NODE_Y, "", "OR", null, false, false,
                        NODE_TYPE_SUCCESS, edgeJson(List.of(new EdgeLink("node_6", ""))), List.of()),
                new NodeConfig("node_5", "失败", FAIL_NODE_X, FAIL_NODE_Y, "", "OR", null, false, false,
                        NODE_TYPE_FAIL, edgeJson(List.of(new EdgeLink("node_2", ""))), List.of()),
                new NodeConfig("node_6", "结束", END_NODE_X, SUCCESS_NODE_Y, "", "OR", null, false, false,
                        NODE_TYPE_END, "", List.of())
        );
        String body = nodes.stream().map(WorkflowBpmnModelSupport::nodeJson).collect(Collectors.joining(",\n"));
        return "[\n" + body + "\n]";
    }

    /**
     * 构建 Flowable BPMN XML。
     *
     * @param processDefinitionKey 流程定义 key
     * @param workflowName         流程名称
     * @param nodes                节点配置
     * @param objectMapper         JSON 组件
     * @return BPMN XML 字节
     */
    static byte[] buildBpmnXml(String processDefinitionKey,
                               String workflowName,
                               List<NodeConfig> nodes,
                               ObjectMapper objectMapper) {
        Graph graph = validateAndBuildGraph(nodes, objectMapper);
        BpmnModel bpmnModel = new BpmnModel();
        Process process = new Process();
        process.setId(processDefinitionKey);
        process.setName(workflowName);
        bpmnModel.addProcess(process);

        process.addFlowElement(startEvent());
        process.addFlowElement(endEvent());
        for (NodeConfig node : graph.nodes().values()) {
            if (isBpmnTaskNode(node.nodeType())) {
                process.addFlowElement(userTask(node));
            }
        }
        int index = 1;
        for (EdgeConfig edge : graph.edges()) {
            process.addFlowElement(sequenceFlow(edge, index++));
        }
        return new BpmnXMLConverter().convertToXML(bpmnModel, StandardCharsets.UTF_8.name());
    }

    /**
     * 校验节点连线并返回图结构。
     *
     * @param nodes        节点配置
     * @param objectMapper JSON 组件
     * @return 图结构
     */
    static Graph validateAndBuildGraph(List<NodeConfig> nodes, ObjectMapper objectMapper) {
        if (nodes == null || nodes.isEmpty()) {
            throw new BusinessException("流程节点不能为空");
        }
        Map<String, NodeConfig> nodeMap = buildNodeMap(nodes);
        List<EdgeConfig> edges = collectEdges(nodeMap.values(), objectMapper);
        validateEdges(nodeMap, edges);
        validateNodeDegrees(nodeMap, edges);
        return new Graph(nodeMap, edges);
    }

    /**
     * 判断节点类型是否支持审批角色。
     *
     * @param nodeType 节点类型
     * @return 是否支持
     */
    static boolean supportsRoleAssignment(String nodeType) {
        return NODE_TYPE_NORMAL.equals(nodeType) || NODE_TYPE_CONDITION.equals(nodeType);
    }

    /**
     * 判断节点类型是否支持指定审批人。
     *
     * @param nodeType 节点类型
     * @return 是否支持
     */
    static boolean supportsApproverUser(String nodeType) {
        return NODE_TYPE_CONDITION.equals(nodeType);
    }

    /**
     * 标准化节点类型。
     *
     * @param value 原始类型
     * @return 标准类型
     */
    static String normalizeNodeType(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return NODE_TYPE_NORMAL;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (NODE_TYPE_CONDITION.equals(upper)
                || NODE_TYPE_SUCCESS.equals(upper)
                || NODE_TYPE_FAIL.equals(upper)
                || NODE_TYPE_START.equals(upper)
                || NODE_TYPE_END.equals(upper)
                || NODE_TYPE_NORMAL.equals(upper)) {
            return upper;
        }
        return NODE_TYPE_NORMAL;
    }

    /**
     * 标准化条件表达式。
     *
     * @param expression 原始表达式
     * @return 标准表达式
     */
    static String normalizeConditionExpression(String expression) {
        String value = trimToNull(expression);
        if (value == null) {
            return "";
        }
        String compact = value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        if (CONDITION_TRUE_EXPRESSION.equals(compact)) {
            return CONDITION_TRUE_EXPRESSION;
        }
        if (CONDITION_FALSE_EXPRESSION.equals(compact)) {
            return CONDITION_FALSE_EXPRESSION;
        }
        return value;
    }

    private static StartEvent startEvent() {
        StartEvent startEvent = new StartEvent();
        startEvent.setId(BPMN_START_EVENT_ID);
        startEvent.setName("开始");
        return startEvent;
    }

    private static EndEvent endEvent() {
        EndEvent endEvent = new EndEvent();
        endEvent.setId(BPMN_END_EVENT_ID);
        endEvent.setName("结束");
        return endEvent;
    }

    private static UserTask userTask(NodeConfig node) {
        UserTask userTask = new UserTask();
        userTask.setId(BPMN_TASK_PREFIX + node.nodeKey());
        userTask.setName(node.nodeName());
        userTask.setDocumentation(taskDocumentation(node));
        return userTask;
    }

    private static String taskDocumentation(NodeConfig node) {
        return "nodeType=" + node.nodeType()
                + ";approverRoleCode=" + blankToEmpty(node.approverRoleCode())
                + ";approverUserId=" + (node.approverUserId() == null ? "" : node.approverUserId())
                + ";triggerActions=" + String.join(",", node.triggerActions() == null ? List.of() : node.triggerActions());
    }

    private static SequenceFlow sequenceFlow(EdgeConfig edge, int index) {
        SequenceFlow sequenceFlow = new SequenceFlow();
        sequenceFlow.setId("flow_" + index + "_" + safeFlowPart(edge.from()) + "_" + safeFlowPart(edge.to()));
        sequenceFlow.setSourceRef(toBpmnElementId(edge.fromNodeType(), edge.from()));
        sequenceFlow.setTargetRef(toBpmnElementId(edge.toNodeType(), edge.to()));
        if (NODE_TYPE_CONDITION.equals(edge.fromNodeType())) {
            sequenceFlow.setConditionExpression(toBpmnConditionExpression(edge.expression()));
        }
        return sequenceFlow;
    }

    private static String toBpmnConditionExpression(String expression) {
        String normalized = normalizeConditionExpression(expression);
        if (CONDITION_TRUE_EXPRESSION.equals(normalized)) {
            return "${" + CONDITION_RESULT_VARIABLE + " == true}";
        }
        if (CONDITION_FALSE_EXPRESSION.equals(normalized)) {
            return "${" + CONDITION_RESULT_VARIABLE + " == false}";
        }
        throw new BusinessException("条件节点连线表达式仅支持 $.result==true 或 $.result==false");
    }

    private static Map<String, NodeConfig> buildNodeMap(List<NodeConfig> nodes) {
        Map<String, NodeConfig> nodeMap = new LinkedHashMap<>();
        for (NodeConfig node : nodes) {
            String nodeKey = trimToNull(node.nodeKey());
            if (!StringUtils.hasText(nodeKey)) {
                throw new BusinessException("节点编码不能为空");
            }
            if (nodeMap.putIfAbsent(nodeKey, node) != null) {
                throw new BusinessException("节点编码重复：" + nodeKey);
            }
        }
        return nodeMap;
    }

    private static List<EdgeConfig> collectEdges(Iterable<NodeConfig> nodes, ObjectMapper objectMapper) {
        List<EdgeConfig> edges = new ArrayList<>();
        for (NodeConfig node : nodes) {
            List<EdgeLink> links = parseEdgeLinks(node.conditionExpression(), objectMapper);
            for (EdgeLink link : links) {
                edges.add(new EdgeConfig(
                        node.nodeKey(),
                        link.to(),
                        normalizeConditionExpression(link.expression()),
                        node.nodeType(),
                        ""
                ));
            }
        }
        return edges;
    }

    private static List<EdgeLink> parseEdgeLinks(String expressionJson, ObjectMapper objectMapper) {
        String value = trimToNull(expressionJson);
        if (value == null) {
            return List.of();
        }
        try {
            List<EdgeLink> links = objectMapper.readValue(value, new TypeReference<>() {
            });
            return links == null ? List.of() : links;
        } catch (Exception ex) {
            throw new BusinessException("流程连线配置数据损坏");
        }
    }

    private static void validateEdges(Map<String, NodeConfig> nodeMap, List<EdgeConfig> edges) {
        if (edges.isEmpty()) {
            throw new BusinessException("流程节点之间缺少连线");
        }
        Set<String> edgeKeys = new HashSet<>();
        for (int i = 0; i < edges.size(); i++) {
            EdgeConfig edge = edges.get(i);
            NodeConfig source = nodeMap.get(edge.from());
            NodeConfig target = nodeMap.get(edge.to());
            if (source == null || target == null) {
                throw new BusinessException("流程连线引用了不存在的节点");
            }
            if (NODE_TYPE_END.equals(source.nodeType()) || NODE_TYPE_START.equals(target.nodeType())) {
                throw new BusinessException("流程连线方向非法");
            }
            String edgeKey = edge.from() + "->" + edge.to();
            if (!edgeKeys.add(edgeKey)) {
                throw new BusinessException("流程连线重复：" + edgeKey);
            }
            edges.set(i, edge.withNodeTypes(source.nodeType(), target.nodeType()));
        }
    }

    private static void validateNodeDegrees(Map<String, NodeConfig> nodeMap, List<EdgeConfig> edges) {
        Map<String, List<EdgeConfig>> outgoing = groupOutgoing(edges);
        Map<String, Integer> incomingCount = countIncoming(edges);
        int startCount = 0;
        int endCount = 0;
        int conditionCount = 0;
        for (NodeConfig node : nodeMap.values()) {
            String nodeType = node.nodeType();
            startCount += NODE_TYPE_START.equals(nodeType) ? 1 : 0;
            endCount += NODE_TYPE_END.equals(nodeType) ? 1 : 0;
            conditionCount += NODE_TYPE_CONDITION.equals(nodeType) ? 1 : 0;
            validateNodeDegree(node, outgoing.getOrDefault(node.nodeKey(), List.of()),
                    incomingCount.getOrDefault(node.nodeKey(), 0));
        }
        if (startCount != 1 || endCount != 1) {
            throw new BusinessException("流程必须且只能包含一个开始节点和一个结束节点");
        }
        if (conditionCount < 1) {
            throw new BusinessException("流程至少需要一个条件节点");
        }
    }

    private static void validateNodeDegree(NodeConfig node, List<EdgeConfig> outgoing, int incomingCount) {
        String nodeType = node.nodeType();
        if (NODE_TYPE_START.equals(nodeType)) {
            requireDegree(node, incomingCount == 0 && outgoing.size() == 1);
            return;
        }
        if (NODE_TYPE_END.equals(nodeType)) {
            requireDegree(node, incomingCount > 0 && outgoing.isEmpty());
            return;
        }
        requireDegree(node, incomingCount > 0 && !outgoing.isEmpty());
        if (NODE_TYPE_CONDITION.equals(nodeType)) {
            validateConditionEdges(node, outgoing);
        } else if (outgoing.size() > 1) {
            throw new BusinessException("节点【" + node.nodeName() + "】只能配置一条后续连线");
        }
    }

    private static void validateConditionEdges(NodeConfig node, List<EdgeConfig> outgoing) {
        if (outgoing.size() != 2) {
            throw new BusinessException("条件节点【" + node.nodeName() + "】必须配置 true 和 false 两条分支");
        }
        Set<String> expressions = new LinkedHashSet<>();
        for (EdgeConfig edge : outgoing) {
            String expression = normalizeConditionExpression(edge.expression());
            if (!CONDITION_TRUE_EXPRESSION.equals(expression) && !CONDITION_FALSE_EXPRESSION.equals(expression)) {
                throw new BusinessException("条件节点【" + node.nodeName() + "】分支表达式必须为 $.result==true 或 $.result==false");
            }
            expressions.add(expression);
        }
        if (expressions.size() != 2) {
            throw new BusinessException("条件节点【" + node.nodeName() + "】必须同时配置 true 和 false 分支");
        }
    }

    private static void requireDegree(NodeConfig node, boolean matched) {
        if (!matched) {
            throw new BusinessException("节点【" + node.nodeName() + "】未完整连线");
        }
    }

    private static Map<String, List<EdgeConfig>> groupOutgoing(List<EdgeConfig> edges) {
        Map<String, List<EdgeConfig>> outgoing = new LinkedHashMap<>();
        for (EdgeConfig edge : edges) {
            outgoing.computeIfAbsent(edge.from(), key -> new ArrayList<>()).add(edge);
        }
        return outgoing;
    }

    private static Map<String, Integer> countIncoming(List<EdgeConfig> edges) {
        Map<String, Integer> incomingCount = new LinkedHashMap<>();
        for (EdgeConfig edge : edges) {
            incomingCount.merge(edge.to(), 1, Integer::sum);
        }
        return incomingCount;
    }

    private static boolean isBpmnTaskNode(String nodeType) {
        return !NODE_TYPE_START.equals(nodeType) && !NODE_TYPE_END.equals(nodeType);
    }

    private static String toBpmnElementId(String nodeType, String nodeKey) {
        if (NODE_TYPE_START.equals(nodeType)) {
            return BPMN_START_EVENT_ID;
        }
        if (NODE_TYPE_END.equals(nodeType)) {
            return BPMN_END_EVENT_ID;
        }
        return BPMN_TASK_PREFIX + nodeKey;
    }

    private static String safeFlowPart(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private static String blankToEmpty(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    private static String nodeJson(NodeConfig node) {
        return "  {\"nodeKey\":\"" + node.nodeKey()
                + "\",\"nodeName\":\"" + node.nodeName()
                + "\",\"x\":" + node.x()
                + ",\"y\":" + node.y()
                + ",\"approverRoleCode\":\"" + blankToEmpty(node.approverRoleCode())
                + "\",\"roleSignMode\":\"" + blankToEmpty(node.roleSignMode())
                + "\",\"approverUserId\":" + (node.approverUserId() == null ? "null" : node.approverUserId())
                + ",\"allowReject\":" + node.allowReject()
                + ",\"allowUnapprove\":" + node.allowUnapprove()
                + ",\"nodeType\":\"" + node.nodeType()
                + "\",\"conditionExpression\":\"" + escapeJsonString(node.conditionExpression())
                + "\",\"triggerActions\":" + stringArrayJson(node.triggerActions()) + "}";
    }

    private static String edgeJson(List<EdgeLink> links) {
        return links.stream()
                .map(link -> "{\"to\":\"" + link.to() + "\",\"expression\":\"" + link.expression() + "\"}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String stringArrayJson(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.stream()
                .map(value -> "\"" + escapeJsonString(value) + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String escapeJsonString(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /** 流程节点配置模型，承载页面保存的节点属性和连线 JSON。 */
    record NodeConfig(String nodeKey,
                      String nodeName,
                      Integer x,
                      Integer y,
                      String approverRoleCode,
                      String roleSignMode,
                      Long approverUserId,
                      boolean allowReject,
                      boolean allowUnapprove,
                      String nodeType,
                      String conditionExpression,
                      List<String> triggerActions) {
    }

    /** 流程图结构，承载节点和连线。 */
    record Graph(Map<String, NodeConfig> nodes, List<EdgeConfig> edges) {
    }

    private record EdgeLink(String to, String expression) {
    }

    private record EdgeConfig(String from, String to, String expression, String fromNodeType, String toNodeType) {
        private EdgeConfig withNodeTypes(String newFromNodeType, String newToNodeType) {
            return new EdgeConfig(from, to, expression, newFromNodeType, newToNodeType);
        }
    }
}
