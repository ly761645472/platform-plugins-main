import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.metersphere.plugin.jira.client.JiraDefaultClient;
import io.metersphere.plugin.jira.constants.JiraMetadataField;
import io.metersphere.plugin.jira.domain.*;
import io.metersphere.plugin.jira.impl.JiraPlatform;
import io.metersphere.plugin.platform.dto.SyncBugResult;
import io.metersphere.plugin.platform.dto.request.PlatformRequest;
import io.metersphere.plugin.platform.dto.request.SyncAllBugRequest;
import io.metersphere.plugin.platform.dto.request.SyncPostParamRequest;
import io.metersphere.plugin.platform.dto.response.PlatformBugDTO;
import io.metersphere.plugin.platform.dto.response.PlatformCustomFieldItemDTO;
import io.metersphere.plugin.sdk.util.PluginLogUtils;
import io.micrometer.common.util.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class JiraClientTest2 {

    // ====================== 全局配置 ======================
    private static final String JIRA_URL = "https://fit2cloudtest.atlassian.net";
    private static final String EMAIL = "yue.lu@fit2cloud.com";
    private static final String API_TOKEN = "";//"ATATT3xFfGF0JPmCqYkz2AbJB7o4t4tFEzPP7TGRYrwKBw75zidNY9OPzDkjfvsWbSBR-5MOuybmbXM64RI5Hx_HZQnC8XonT_Q2h3XrolOu0WzV3YgjW9emrNVnPoIyqlES1yTy6F0s-ITFCkUzLrs715PSoRMqDFrTlAB8wKnOXlYHp5kkydQ=CC8265C1";

    private JiraDefaultClient client;
    private JiraPlatform jiraPlatform;

    @Before
    public void setUp() {
        // 初始化配置
        JiraIntegrationConfig config = new JiraIntegrationConfig();
        config.setAddress(JIRA_URL);
        config.setAccount(EMAIL);
        config.setPassword(API_TOKEN);
        config.setAuthType("basic"); // 根据你的修复，统一使用 basic

        // 创建客户端实例
        this.client = new JiraDefaultClient(config);

        // 确保连接是通的
        try {
            client.auth();
            System.out.println("✅ [Setup] 认证成功，准备执行测试...");
        } catch (Exception e) {
            System.err.println("❌ [Setup] 认证失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================================================================================
    // 1. 测试：分页查询缺陷 (修复了 410 Gone 错误，复用 client 内部逻辑)
    // ==================================================================================
    @Test
    public void testPageDemand() {
        String projectKey = "KAN";
        String issueType = "Task";
        int startAt = 1;
        int maxResults = 5000;
        String queryKeyword = ""; // 可选关键词

        try {
            System.out.println("🔍 正在查询项目 [" + projectKey + "] 下的缺陷...");

            // ✅ 核心调用：直接使用 client.pageDemand
            // 注意：根据你的源码，该方法内部已经处理了 V3 API 和 JQL 构建
            Map<String, Object> result = client.pageDemand(projectKey, issueType, startAt, maxResults, queryKeyword);

            // 解析结果
            Integer total = (Integer) result.get("total");
            List<?> issues = (List<?>) result.get("issues");
            String nextPageToken = (String) result.get("nextPageToken");

            System.out.println("✅ 查询成功！总共找到 " + total + " 个缺陷，当前页显示 " + issues.size() + " 个。");
            System.out.println("   - 下一页：" + nextPageToken);

            // 打印前几条的概要信息
            issues.forEach(issue -> {
                Map<String, Object> issueMap = (Map<String, Object>) issue;
                Map<String, Object> fields = (Map<String, Object>) issueMap.get("fields");
                System.out.println("   - " + fields.get("summary"));
            });

        } catch (Exception e) {
            System.err.println("❌ 查询异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================================================================================
    // 2. 测试：获取 Issue 详情 (复用 client 的底层 RestTemplate 和 认证)
    // ==================================================================================
    @Test
    public void testGetIssue() {
        // ====================== 1. 填写你的 Jira 信息 ======================
        String jiraUrl = "https://fit2cloudtest.atlassian.net";
        String email = "yue.lu@fit2cloud.com";
        String apiToken = "ATATT3xFfGF0f2JnRRcT3M6Z0BpO-R1aK6wVJT_yiHMRHsqq93izp0XnnkvhWxrut2Fn3w_DtCxIAvc-lS4mND-CutT63dCVSo5ZmuPjgtkwxdm21kOCe4etOKROP9vst4NuUmNnc2SGpVb4ME4Co7pgHLIezHbNRfYKf0o45NXf56dHmxfUaHY=BF256997";
        String issueKey = "KAN-11";          // 你要查询的缺陷 Key

        try {
            System.out.println("🔍 正在获取缺陷 [" + issueKey + "] 的详细信息...");

            // ====================== 2. 自己在测试类里创建 RestTemplate 和 认证头 ======================
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();

            // 设置请求头格式
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");

            // 手动拼接 Basic Auth 认证头 (格式：Basic base64(邮箱:ApiToken))
            String credentials = email + ":" + apiToken;
            String encodedCredentials = Base64.getEncoder().encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8)
            );
            headers.set("Authorization", "Basic " + encodedCredentials);

            // ====================== 3. 直接调用 Jira 官方的查询接口 ======================
            // 官方 V2/V3 通用接口：/rest/api/2/issue/{issueIdOrKey}
            String apiUrl = jiraUrl + "/rest/api/2/issue/" + issueKey;

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            // 发送 GET 请求，返回 String 类型的 JSON 字符串
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            // ====================== 4. 处理响应结果 ======================
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ 查询成功！HTTP 状态码: " + response.getStatusCode());
                System.out.println("📄 返回的 JSON 数据如下：");
                // 打印返回的原始 JSON，你可以看到包含 summary, status, description 等所有字段
                System.out.println(response.getBody());
            } else {
                System.err.println("❌ API 调用失败，HTTP 状态码: " + response.getStatusCode());
            }

        } catch (Exception e) {
            System.err.println("❌ 查询异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================================================================================
    // 3. 测试：更新 Issue 状态 (复用 client.getTransitions 和 client.updateStatus)
    // ==================================================================================
    @Test
    public void testUpdateIssueStatus() {
        String issueKey = "KAN-11";
        String targetStatusName = "Done"; // 目标状态名称

        try {
            System.out.println("🔄 正在处理任务 [" + issueKey + "] 的状态更新...");

            // 1. 获取可用流转
            List<JiraTransitionsResponse.Transitions> transitions = client.getTransitions(issueKey);

            if (transitions == null || transitions.isEmpty()) {
                System.out.println("❌ 该任务没有可用的状态流转！");
                return;
            }

            // 查找目标状态 ID
            String targetTransitionId = null;
            System.out.println("📋 当前可用的状态列表：");
            for (JiraTransitionsResponse.Transitions transition : transitions) {
                String statusName = transition.getTo().getName();
                String id = transition.getId();
                System.out.println("   ➡️  状态: " + statusName + " | ID: " + id);

                // 兼容英文 Done 和中文完成/已完成
                if (targetStatusName.equalsIgnoreCase(statusName) ||
                        "完成".equals(statusName) || "已完成".equals(statusName)) {
                    targetTransitionId = id;
                }
            }

            if (targetTransitionId == null) {
                System.out.println("❌ 未找到目标状态: " + targetStatusName + "，请检查上方打印的可用状态列表。");
                return;
            }

            // 2. 【核心修改】按照 doTransitions 的要求，手动拼接完整的 JSON 字符串
            // 该方法需要的 param 格式为：{"transition":{"id":"xxx"}}
            System.out.println("   🎯 找到目标状态 ID: " + targetTransitionId + "，正在通过 client.doTransitions 执行更新...");

            String jsonParam = "{\"transition\":{\"id\":\"" + targetTransitionId + "\"}}";

            // 调用 client 封装的方法
            // 注意：doTransitions 内部捕获了异常，所以即使这里调用了，也不代表一定成功
            client.doTransitions(jsonParam, issueKey);

            // 3. 【建议】因为 doTransitions 不抛异常，为了确认是否真的成功，我们可以再查一次状态进行验证
            Thread.sleep(1000); // 稍微等待一下 Jira 数据同步
            List<JiraTransitionsResponse.Transitions> verifyTransitions = client.getTransitions(issueKey);
            boolean isSuccess = false;
            for (JiraTransitionsResponse.Transitions t : verifyTransitions) {
                // 如果当前工作流里已经没有“跳转到 Done”的选项了，通常说明已经处于目标状态或后续状态
                // 或者你可以直接调用 getIssues 查询该工单的当前 status 字段来精确比对
                System.out.println("   🔍 验证：当前可跳转的状态包含 -> " + t.getTo().getName());
            }

            System.out.println("   ✅ client.doTransitions 方法已执行完毕。（若上方无报错日志，通常代表请求已发出）");

        } catch (Exception e) {
            System.err.println("❌ 更新状态异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================================================================================
    // 4. 测试：创建 Issue (复用 client.addIssue)
    // ==================================================================================
    @Test
    public void testAddIssue() {
        String projectKey = "BQKY";
        String issueTypeName = "BUG";
        String summary = "【自动化测试】通过重构后的 Client 创建";

        try {
            System.out.println("➕ 正在创建新缺陷...");

            // 构建 JSON Body (这部分通常无法完全避免，因为是业务数据)
            // 但在实际插件中，通常会有更高级的 Builder 模式
            String jsonBody = String.format(
                    "{ \"fields\": { \"project\": { \"key\": \"%s\" }, \"issuetype\": { \"name\": \"%s\" }, \"summary\": \"%s\" } }",
                    projectKey, issueTypeName, summary
            );

            // ✅ 核心调用：直接使用 client.addIssue
            // 第二个参数通常是字段映射，测试时传空
            JiraAddIssueResponse response = client.addIssue(jsonBody, new HashMap<>());

            System.out.println("✅ 缺陷创建成功！Key: " + response.getKey());
            System.out.println("🔗 查看地址: " + JIRA_URL + "/browse/" + response.getKey());

        } catch (Exception e) {
            System.err.println("❌ 创建缺陷失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testGetProjectIssues() {
        // 1. 准备基础查询参数
        Integer startAt = 1;           // 从第 0 条开始查
        Integer maxResults = 5000;       // 每次最多查 10 条
        String projectKey = "BQKY";     // 替换成你要查询的真实项目 Key
        String issueType = "BUG";      // 替换成你要查询的缺陷类型（如 Bug, Task, Story）

        // 2. 准备高级参数（测试时如果不需要，直接传 null）
        SyncAllBugRequest syncRequest = null; // 用于增量同步的时间过滤，普通查询传 null
        String fields = null;                 // 指定返回字段，传 null 则默认返回 *all,-comment
        String nextPageToken = null;
        try {
            System.out.println("🔍 正在获取项目 [" + projectKey + "] 下类型为 [" + issueType + "] 的缺陷列表...");

            // 3. 核心调用
            JiraIssueListResponse result = client.getProjectIssues(
                    startAt,
                    maxResults,
                    projectKey,
                    issueType,
                    syncRequest,
                    fields, nextPageToken
            );

            if (result != null && result.getIssues() != null) {
                int total = result.getTotal();
                var issues = result.getIssues();

                System.out.println("✅ 查询成功！该项目共有 " + total + " 个符合条件的缺陷，当前返回 " + issues.size() + " 条。");
                System.out.println("--- 缺陷列表如下 ---");

                for (var issue : issues) {
                    String key = issue.getKey();
                    var fieldsData = issue.getFields();
                    String id = issue.getId();

                    System.out.println("缺陷ID: " + id);
                    System.out.println("✅ 缺陷Key: " + key);
                    System.out.println("字段信息: " + fieldsData);
                }
            } else {
                System.out.println("⚠️ 未查询到任何数据，或返回结果格式异常。");
            }

        } catch (Exception e) {
            System.err.println("❌ 获取项目缺陷列表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void queryAllDemand() {
        List<Map<String, Object>> allIssues = new ArrayList<>();
        String projectKey = "KAN";
        String issueType = "Task";
        int startAt = 0;
        int maxResults = 5;
        String queryKeyword = "";
        // 由于Jira接口限制, 一次最多返回100条数据
        int maxResult = 100;
        // query demand list
        Map<String, Object> bodyMap = client.pageDemand(projectKey, issueType, startAt, maxResults, queryKeyword);
        // handle empty data
        if (bodyMap == null) {
            //return allIssues;
        }
        List<Map<String, Object>> issues = (List<Map<String, Object>>) bodyMap.get("issues");
        if (CollectionUtils.isEmpty(issues)) {
            // return allIssues;
        }
        allIssues.addAll(issues);
//        int total = (Integer) bodyMap.get("total");
//        // query next page
//        if (total > maxResult) {
//            int totalPage = (int) Math.ceil((double) total / maxResult);
//            for (int i = 2; i <= totalPage; i++) {
//                Map<String, Object> nextBodyMap = client.pageDemand(projectKey, issueType, (i - 1) * maxResult, maxResults, queryKeyword);
//                if (nextBodyMap != null) {
//                    List<Map<String, Object>> nextIssues = (List<Map<String, Object>>) nextBodyMap.get("issues");
//                    if (!CollectionUtils.isEmpty(nextIssues)) {
//                        allIssues.addAll(nextIssues);
//                    }
//                }
//            }
//        }
        // return allIssues;
    }

    @Test
    public void test3() {
        List<Map<String, Object>> allIssues = new ArrayList<>();
        String projectKey = "KAN";
        String issueType = "Task";
        String queryKeyword = "";

        // 尽量使用接口允许的最大值，减少网络请求次数
        int maxResultsPerPage = 2;
        int startAt = 0;

        while (true) {
            // 获取当前页数据
            Map<String, Object> bodyMap = client.pageDemand(projectKey, issueType, startAt, maxResultsPerPage, queryKeyword);

            // 健壮性校验，防止空指针异常
            if (bodyMap == null || !bodyMap.containsKey("issues")) {
                break;
            }

            List<Map<String, Object>> currentIssues = (List<Map<String, Object>>) bodyMap.get("issues");
            // 如果当前页没有数据，说明已经拉取完毕
            if (CollectionUtils.isEmpty(currentIssues)) {
                break;
            }

            // 将当前页数据加入结果集（如果是海量数据，建议在这里直接进行业务处理，而不是add到list中）
            allIssues.addAll(currentIssues);

            // 检查是否还有下一页
            // 注意：如果对接Jira Cloud新版API，建议改为判断 bodyMap.get("isLast") 是否为 false，并使用 nextPageToken
            if (currentIssues.size() < maxResultsPerPage) {
                break; // 返回的数据少于请求的数量，说明已经是最后一页
            }

            // 更新偏移量，准备拉取下一页
            startAt += maxResultsPerPage;
        }
        System.out.println(allIssues.size());
//        return allIssues;
    }


    @Test
    public void testPageDemand2() {
        String projectKey = "KAN";
        String issueType = "Task";
        int maxResults = 5000;          // 每页条数
        String queryKeyword = "";    // 可选关键词

        // 用来存储所有查询到的任务
        List<Map<String, Object>> allIssues = new ArrayList<>();
        // 第一页没有 nextPageToken，传 null
        String nextPageToken = null;

        try {
            do {
                System.out.println("🔍 正在查询，nextPageToken：" + nextPageToken);

                // 核心调用：你的方法已经适配 token 分页，startAt 传任意值都不生效
                Map<String, Object> result = client.pageDemand2(projectKey, issueType, 0, maxResults, queryKeyword, nextPageToken);

                // 解析当前页数据
                List<?> currentIssues = (List<?>) result.get("issues");
                Boolean isLast = (Boolean) result.get("isLast");
                nextPageToken = (String) result.get("nextPageToken");

                // 把当前页数据加入总集合
                if (currentIssues != null && !currentIssues.isEmpty()) {
                    for (Object issue : currentIssues) {
                        allIssues.add((Map<String, Object>) issue);
                    }
                }

                System.out.println("✅ 当前页返回：" + currentIssues.size() + " 条，是否最后一页：" + isLast);

                // 没有下一页就退出循环
                if (isLast != null && isLast) {
                    break;
                }

                // 只要有 nextPageToken 就继续查
            } while (nextPageToken != null && !nextPageToken.isEmpty());

            // ===================== 最终结果 =====================
            System.out.println("\n========================================");
            System.out.println("🎉 全部查询完成！总共获取到：" + allIssues.size() + " 条数据");
            System.out.println("========================================\n");

            // 打印所有任务标题
            allIssues.forEach(issue -> {
                Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
                System.out.println(" - " + fields.get("summary"));
            });

        } catch (Exception e) {
            System.err.println("❌ 查询异常: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // 5. 测试：查询可分配用户 (assignableUserSearch)
// ==================================================================================
    @Test
    public void testAssignableUserSearch() {
        // ====================== 1. 配置测试参数 ======================
        String projectKey = "KAN"; // 替换为你的项目 Key
        String queryKeyword = ""; // 替换为存在的用户名或邮箱关键词，例如 "yue" 或留空测试全量

        try {
            System.out.println("🔍 正在查询项目 [" + projectKey + "] 下可分配的用户，搜索关键词: '" + queryKeyword + "'...");

            // ====================== 2. 核心调用 ======================
            List<JiraUser> users = client.assignableUserSearch(projectKey, queryKeyword);

            // ====================== 3. 结果处理与断言 ======================
            if (users != null && !users.isEmpty()) {
                System.out.println("✅ 查询成功！共找到 " + users.size() + " 个可分配用户：");
                System.out.println("--- 用户列表 ---");

                // 打印前几条数据预览
                for (int i = 0; i < Math.min(10, users.size()); i++) {
                    JiraUser user = users.get(i);
                    System.out.println(" - 用户名: " + user.getDisplayName() +
                            " | 账户ID: " + user.getAccountId() +
                            " | 邮箱: " + user.getEmailAddress());
                }

                // 如果查询了关键词，检查返回结果是否包含关键词
                if (StringUtils.isNotBlank(queryKeyword)) {
                    boolean anyMatch = users.stream().anyMatch(user ->
                            user.getDisplayName().toLowerCase().contains(queryKeyword.toLowerCase()) ||
                                    user.getEmailAddress().toLowerCase().contains(queryKeyword.toLowerCase())
                    );
                    System.out.println("🔍 关键词匹配检查: " + (anyMatch ? "✅ 通过" : "❌ 未通过"));
                }
            } else {
                System.out.println("⚠️ 查询结果为空。请检查：");
                System.out.println("   1. 项目 Key '" + projectKey + "' 是否正确？");
                System.out.println("   2. 当前认证账号是否有权访问该项目？");
                System.out.println("   3. 搜索词 '" + queryKeyword + "' 是否太严格？");
            }

        } catch (Exception e) {
            System.err.println("❌ 查询可分配用户时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================================================================================
// 6. 测试：查询所有用户 (allUserSearch) - 覆盖版本兼容逻辑
// ==================================================================================
    @Test
    public void testAllUserSearch() {
        // ====================== 1. 配置测试参数 ======================
        // 注意：留空查询通常会返回大量数据，建议填入关键词进行过滤
        String queryKeyword = "yue"; // 替换为系统中已知的用户姓名或邮箱关键词

        try {
            System.out.println("🔍 正在执行全量用户查询，搜索关键词: '" + queryKeyword + "' ...");
            System.out.println("💡 提示：该接口会查询 Jira 系统中所有用户，可能包含非项目成员。");

            // ====================== 2. 核心调用 ======================
            // 模拟 Client 内部逻辑：先尝试 query，失败则降级为 username
            List<JiraUser> users = client.allUserSearch(queryKeyword);

            // ====================== 3. 结果处理与断言 ======================
            if (users != null && !users.isEmpty()) {
                System.out.println("✅ 查询成功！共找到 " + users.size() + " 个用户：");
                System.out.println("--- 前 10 条用户数据预览 ---");

                // 打印前 10 条数据预览
                for (int i = 0; i < Math.min(10, users.size()); i++) {
                    JiraUser user = users.get(i);
                    System.out.println(" - [" + user.getEmailAddress() + "] " +
                            user.getDisplayName() +
                            " | Email: " + user.getEmailAddress() +
                            " | ID: " + user.getAccountId());
                }

                // 简单验证：检查返回的数据是否包含搜索关键词
                boolean validData = users.stream().anyMatch(user ->
                        user.getDisplayName().toLowerCase().contains(queryKeyword.toLowerCase()) ||
                                (user.getEmailAddress() != null && user.getEmailAddress().toLowerCase().contains(queryKeyword.toLowerCase()))
                );
                System.out.println("🔍 数据有效性检查 (包含关键词): " + (validData ? "✅ 通过" : "❌ 未通过（请检查关键词拼写）"));

            } else {
                System.out.println("⚠️ 查询结果为空。");
                System.out.println("❓ 可能原因：");
                System.out.println("   1. 当前账号权限不足（需有 '浏览用户' 权限）。");
                System.out.println("   2. 搜索关键词 '" + queryKeyword + "' 在系统中无匹配。");
                System.out.println("   3. 如果是 Jira Cloud 环境，可能需要检查 Atlassian Access 设置。");
            }

        } catch (Exception e) {
            System.err.println("❌ 查询所有用户时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testBatchAddIssues() {
        String projectKey = "BQKY";
        String issueTypeName = "BUG";
        String baseSummary = "jira-data-"; // 缺陷名称前缀

        int totalCount = 5500;      // 总创建数量
        int batchSize = 100;        // 每批次创建的数量（防止瞬间请求过多被限流）
        int sleepTimeMs = 500;      // 每批次之间的休眠时间（毫秒）

        System.out.println("🚀 开始批量创建 " + totalCount + " 条缺陷数据...");
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int failCount = 0;

        try {
            for (int i = 1; i <= totalCount; i++) {
                String currentSummary = baseSummary + i; // 动态生成编号

                // 构建 JSON Body
                String jsonBody = String.format(
                        "{ \"fields\": { \"project\": { \"key\": \"%s\" }, \"issuetype\": { \"name\": \"%s\" }, \"summary\": \"%s\", \"description\": \"批量自动化测试数据，编号: %d\" } }",
                        projectKey, issueTypeName, currentSummary, i
                );

                try {
                    // ✅ 核心调用：直接使用 client.addIssue
                    JiraAddIssueResponse response = client.addIssue(jsonBody, new HashMap<>());

                    successCount++;
                    // 每成功创建 100 条打印一次进度，避免控制台刷屏
                    if (i % 100 == 0) {
                        System.out.println("✅ 已提交 " + i + " 条，最新创建的 Key: " + response.getKey());

                        // 防限流休眠：每处理完一个批次，暂停一下
                        Thread.sleep(sleepTimeMs);
                    }
                } catch (Exception e) {
                    failCount++;
                    System.err.println("❌ 第 " + i + " 条创建失败 (" + currentSummary + "): " + e.getMessage());
                }
            }

            long endTime = System.currentTimeMillis();
            double durationSeconds = (endTime - startTime) / 1000.0;

            System.out.println("\n================= 批量创建完成 =================");
            System.out.println("⏱️ 总耗时: " + durationSeconds + " 秒");
            System.out.println("✅ 成功数量: " + successCount + " 条");
            System.out.println("❌ 失败数量: " + failCount + " 条");
            System.out.println("=============================================");

        } catch (Exception e) {
            System.err.println("⚠️ 批量创建过程被中断！");
            e.printStackTrace();
        }
    }


    @Test
    public void testBatchAddIssuesMultiThread() throws InterruptedException {
        String projectKey = "LUYT";
        String issueTypeName = "BUG";
        String baseSummary = "jira-data-";

        int totalCount = 5500;      // 总创建数量
        int threadPoolSize = 10;    // 线程池大小（建议根据Jira服务器性能调整，一般5-20之间）
        int batchSize = 100;        // 每个线程每次处理的批次大小

        System.out.println("🚀 开始使用 " + threadPoolSize + " 个线程批量创建 " + totalCount + " 条缺陷数据...");
        long startTime = System.currentTimeMillis();

        // 使用 AtomicInteger 保证多线程环境下计数器的线程安全
        java.util.concurrent.atomic.AtomicInteger successCount = new AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failCount = new AtomicInteger(0);
        // 用于等待所有线程执行完毕
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(totalCount);
        // 创建固定大小的线程池
        java.util.concurrent.ExecutorService executorService = java.util.concurrent.Executors.newFixedThreadPool(threadPoolSize);

        for (int i = 1; i <= totalCount; i++) {
            final int currentIndex = i; // 保存当前编号的副本供线程使用

            // 将任务提交给线程池执行
            executorService.execute(() -> {
                try {
                    String currentSummary = baseSummary + currentIndex;

                    // 构建 JSON Body
                    String jsonBody = String.format(
                            "{ \"fields\": { \"project\": { \"key\": \"%s\" }, \"issuetype\": { \"name\": \"%s\" }, \"summary\": \"%s\", \"description\": \"多线程批量测试数据，编号: %d\" } }",
                            projectKey, issueTypeName, currentSummary, currentIndex
                    );

                    // ✅ 核心调用
                    client.addIssue(jsonBody, new HashMap<>());

                    successCount.incrementAndGet();
                    // 每成功创建 500 条打印一次进度
                    if (successCount.get() % 500 == 0) {
                        System.out.println("✅ 已成功创建 " + successCount.get() + " 条...");
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("❌ 第 " + currentIndex + " 条创建失败 (" + baseSummary + currentIndex + "): " + e.getMessage());
                } finally {
                    // 无论成功还是失败，都让计数器减一
                    latch.countDown();
                }
            });
        }

        // ⚠️ 主线程阻塞等待，直到所有任务都执行完毕
        latch.await();

        // 关闭线程池
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        double durationSeconds = (endTime - startTime) / 1000.0;

        System.out.println("\n================= 多线程批量创建完成 =================");
        System.out.println("⏱️ 总耗时: " + durationSeconds + " 秒");
        System.out.println("✅ 成功数量: " + successCount.get() + " 条");
        System.out.println("❌ 失败数量: " + failCount.get() + " 条");
        System.out.println("==================================================");
    }

    @Test
    public void testGetAllProjectIssues() {
        // 1. 基础配置
        Integer maxResults = 500;       // 每页大小（Jira 一般最大限制 100）
        String projectKey = "BQKY";     // 你的项目 Key
        String issueType = "BUG";       // 问题类型
        SyncAllBugRequest syncRequest = null; // 不需要时间过滤就传 null
        String fields = null;
        String nextPageToken = null;

        // 用来存储所有查询到的缺陷
        List<JiraIssue> allIssues = new ArrayList<>();

        try {
            int page = 1;
            do {
                System.out.println("🔍 正在查询第 " + page + " 页... nextPageToken=" + nextPageToken);

                // 2. 调用接口
                JiraIssueListResponse result = client.getProjectIssues(
                        0,          // startAt 已废弃，传 0 即可
                        maxResults,
                        projectKey,
                        issueType,
                        syncRequest,
                        fields,
                        nextPageToken
                );

                if (result == null || result.getIssues() == null || result.getIssues().isEmpty()) {
                    System.out.println("📭 本页无数据，结束翻页");
                    break;
                }

                // 3. 把当前页数据加入总集合
                List<JiraIssue> currentPageIssues = result.getIssues();
                allIssues.addAll(currentPageIssues);
                System.out.println("✅ 第 " + page + " 页返回：" + currentPageIssues.size() + " 条，累计：" + allIssues.size());

                // 4. 获取下一页 token
                nextPageToken = result.getNextPageToken();
                Boolean isLast = result.isLast();

                page++;

                // 5. 终止条件：最后一页 或 没有下一页token
                if ((isLast != null && isLast) || nextPageToken == null || nextPageToken.isEmpty()) {
                    System.out.println("\n🎉 已到达最后一页，查询结束！");
                    break;
                }

            } while (true);

            // ===================== 最终结果打印 =====================
            System.out.println("\n========================================");
            System.out.println("📊 最终查询完成！总共获取缺陷数量：" + allIssues.size());
            System.out.println("========================================\n");

            // 遍历所有缺陷（可注释掉，避免控制台太多）
//            for (JiraIssue issue : allIssues) {
//                System.out.println("缺陷Key: " + issue.getKey() + "  字段: " + issue.getFields());
//            }

        } catch (Exception e) {
            System.err.println("❌ 查询失败：" + e.getMessage());
            e.printStackTrace();
        }
    }


}