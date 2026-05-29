import com.fasterxml.jackson.databind.ObjectMapper;
import io.metersphere.plugin.jira.client.JiraDefaultClient;
import io.metersphere.plugin.jira.domain.JiraAddIssueResponse;
import io.metersphere.plugin.jira.domain.JiraIntegrationConfig;
import io.metersphere.plugin.jira.domain.JiraTransitionsResponse;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JiraClientTest {
/*

    @Test
    public void test() {
        try {
            // ====================== 填写你的 Jira 信息 ======================
            String jiraUrl = "https://fit2cloudtest.atlassian.net";// 替换为你的 Jira 地址
            String email = "yue.lu@fit2cloud.com";                // 替换为你的登录邮箱
            String apiToken = "ATATT3xFfGF0f2JnRRcT3M6Z0BpO-R1aK6wVJT_yiHMRHsqq93izp0XnnkvhWxrut2Fn3w_DtCxIAvc-lS4mND-CutT63dCVSo5ZmuPjgtkwxdm21kOCe4etOKROP9vst4NuUmNnc2SGpVb4ME4Co7pgHLIezHbNRfYKf0o45NXf56dHmxfUaHY=BF256997";                // 替换为你的 API Token

            // 注意：这里改为了 "basic"
            String authType = "basic";

            // 构建配置
            JiraIntegrationConfig config = new JiraIntegrationConfig();
            config.setAddress(jiraUrl);
            config.setAccount(email);
            config.setPassword(apiToken);
            config.setAuthType(authType);

            // 创建客户端
            JiraDefaultClient client = new JiraDefaultClient(config);

            // 测试认证
            System.out.println("正在尝试认证...");
            client.auth(); // 如果不抛出异常，说明成功

            System.out.println("✅ 认证成功！可以正常访问 Jira。");

        } catch (Exception e) {
            System.err.println("❌ 认证失败！");
            System.err.println("错误详情: " + e.getMessage());
            e.printStackTrace();
        }

    }


    @Test
    public void test2() {
        try {
            // ====================== 填写你的 Jira 信息 ======================
            String jiraUrl = "https://fit2cloudtest.atlassian.net";
            String email = "yue.lu@fit2cloud.com";
            String apiToken = "ATATT3xFfGF0f2JnRRcT3M6Z0BpO-R1aK6wVJT_yiHMRHsqq93izp0XnnkvhWxrut2Fn3w_DtCxIAvc-lS4mND-CutT63dCVSo5ZmuPjgtkwxdm21kOCe4etOKROP9vst4NuUmNnc2SGpVb4ME4Co7pgHLIezHbNRfYKf0o45NXf56dHmxfUaHY=BF256997";
            String projectKey = "KAN";
            String issueType = "Subtask";

            // 构建配置
            JiraIntegrationConfig config = new JiraIntegrationConfig();
            config.setAddress(jiraUrl);
            config.setAccount(email);
            config.setPassword(apiToken);

            //  关键修复：这里必须是 "basic"，而不是 "bearer"
            config.setAuthType("basic");

            // 创建客户端
            JiraDefaultClient client = new JiraDefaultClient(config);

            // 测试修复后的 pageDemand 方法
            Map<String, Object> result = client.pageDemand(
                    projectKey,
                    issueType,
                    0,
                    5,
                    ""
            );

            System.out.println("✅ 测试成功！问题已修复");
            System.out.println("返回数据条数：" + result.get("total"));

        } catch (Exception e) {
            System.err.println("❌ 测试失败");
            e.printStackTrace();
        }
    }


    @Test
    public void testGetAllIssueTypes() {
        try {
            String jiraUrl = "https://fit2cloudtest.atlassian.net";
            String email = "yue.lu@fit2cloud.com";
            String apiToken = "ATATT3xFfGF0f2JnRRcT3M6Z0BpO-R1aK6wVJT_yiHMRHsqq93izp0XnnkvhWxrut2Fn3w_DtCxIAvc-lS4mND-CutT63dCVSo5ZmuPjgtkwxdm21kOCe4etOKROP9vst4NuUmNnc2SGpVb4ME4Co7pgHLIezHbNRfYKf0o45NXf56dHmxfUaHY=BF256997";
            String projectKey = "KAN"; // 你要查询的项目 Key

            RestTemplate restTemplate = new RestTemplate();

            // 构建 Basic Auth 请求头
            String credentials = email + ":" + apiToken;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedCredentials);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 调用获取项目详情的 API
            String url = jiraUrl + "/rest/api/3/project/" + projectKey;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            // 解析返回的 JSON（这里简单打印出来，你可以用 Jackson/Fastjson 进一步解析）
            System.out.println("✅ 成功获取项目信息！");
            System.out.println("该项目支持的所有 IssueType 如下：");

            // 提取并打印 issueTypes 字段的内容
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = mapper.readValue(response.getBody(), Map.class);
            List<Map<String, Object>> issueTypes = (List<Map<String, Object>>) map.get("issueTypes");

            for (Map<String, Object> type : issueTypes) {
                // subtask 为 false 表示是标准任务类型，true 表示是子任务
                System.out.println("- " + type.get("name") + " (ID: " + type.get("id") + ", isSubtask: " + type.get("subtask") + ")");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAddIssue() {
        try {
            // 1. ====================== 填写你的 Jira 信息 ======================
            String jiraUrl = "https://fit2cloudtest.atlassian.net";
            String email = "yue.lu@fit2cloud.com";
            String apiToken = "ATATT3xFfGF0f2JnRRcT3M6Z0BpO-R1aK6wVJT_yiHMRHsqq93izp0XnnkvhWxrut2Fn3w_DtCxIAvc-lS4mND-CutT63dCVSo5ZmuPjgtkwxdm21kOCe4etOKROP9vst4NuUmNnc2SGpVb4ME4Co7pgHLIezHbNRfYKf0o45NXf56dHmxfUaHY=BF256997";

            // 2. ====================== 填写目标项目和 Issue 信息 ======================
            String projectKey = "KAN";     // 目标项目 Key
            String issueTypeName = "Task";  // 缺陷类型 (确保该类型在项目中存在)
            String issueSummary = "【自动化测试】这是一条通过 API 创建的缺陷"; // 缺陷标题

            // 3. ====================== 构建配置并初始化客户端 ======================
            JiraIntegrationConfig config = new JiraIntegrationConfig();
            config.setAddress(jiraUrl);
            config.setAccount(email);
            config.setPassword(apiToken);
            config.setAuthType("basic"); // 根据你的修复，这里必须是 basic

            JiraDefaultClient client = new JiraDefaultClient(config);

            // 4. ====================== 构建请求 Body (JSON 字符串) ======================
            // 注意：Jira API 2.0 创建 Issue 的标准结构
            String jsonBody = String.format(
                    "{ \"fields\": { \"project\": { \"key\": \"%s\" }, \"issuetype\": { \"name\": \"%s\" }, \"summary\": \"%s\" } }",
                    projectKey, issueTypeName, issueSummary
            );

            // 5. ====================== 调用 addIssue 方法 ======================
            // 第二个参数是 fieldNameMap，用于错误提示映射，测试时可传 null 或空 Map
            JiraAddIssueResponse response = client.addIssue(jsonBody, new HashMap<>());

            // 6. ====================== 输出结果 ======================
            System.out.println("✅ 缺陷创建成功！");
            System.out.println("缺陷 Key: " + response.getKey());
            System.out.println("缺陷 URL: " + jiraUrl + "/browse/" + response.getKey());

        } catch (Exception e) {
            System.err.println("❌ 缺陷创建失败: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Test
    public void testUpdateIssueStatus() {
        try {
            // ====================== 1. 填写你的 Jira 信息 ======================
            String jiraUrl = "https://fit2cloudtest.atlassian.net";
            String email = "yue.lu@fit2cloud.com";
            String apiToken = "ATATT3xFfGF0f2JnRRcT3M6Z0BpO-R1aK6wVJT_yiHMRHsqq93izp0XnnkvhWxrut2Fn3w_DtCxIAvc-lS4mND-CutT63dCVSo5ZmuPjgtkwxdm21kOCe4etOKROP9vst4NuUmNnc2SGpVb4ME4Co7pgHLIezHbNRfYKf0o45NXf56dHmxfUaHY=BF256997";
            String issueKey = "KAN-11";          // 你要更新状态的任务编号

            // ====================== 2. 构建配置并初始化客户端 ======================
            JiraIntegrationConfig config = new JiraIntegrationConfig();
            config.setAddress(jiraUrl);
            config.setAccount(email);
            config.setPassword(apiToken);
            config.setAuthType("basic");

            JiraDefaultClient client = new JiraDefaultClient(config);

            // ====================== 3. 获取当前可用的状态流转列表 ======================
            System.out.println("正在查询任务 [" + issueKey + "] 可流转的状态...");
            List<JiraTransitionsResponse.Transitions> transitions = client.getTransitions(issueKey);

            if (transitions == null || transitions.isEmpty()) {
                System.out.println("❌ 该任务没有可用的状态流转！");
                return;
            }

            // 打印并寻找目标状态的 ID
            System.out.println("✅ 可用状态列表如下：");
            String targetTransitionId = null;
            for (JiraTransitionsResponse.Transitions transition : transitions) {
                String id = transition.getId();
                String statusName = transition.getTo().getName();
                System.out.println("  - 状态名称: " + statusName + " | 对应ID: " + id);

                // 匹配你想要的目标状态（根据实际控制台打印结果调整）
                if ("Done".equalsIgnoreCase(statusName) || "完成".equals(statusName)) {
                    targetTransitionId = id;
                }
            }

            // ====================== 4. 独立调用 REST API 执行状态更新 ======================
            if (targetTransitionId != null) {
                System.out.println("\n正在将任务 [" + issueKey + "] 更新为 [Done] 状态...");

                // 【核心修改】自己在测试类里创建 RestTemplate 和 认证头
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                // 手动拼接 Basic Auth 认证头 (格式：Basic base64(邮箱:ApiToken))
                String credentials = email + ":" + apiToken;
                String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
                headers.set("Authorization", "Basic " + encodedCredentials);

                // 构建请求体（Jira 过渡接口的标准 JSON 格式）
                Map<String, Object> bodyMap = new HashMap<>();
                Map<String, Object> transitionMap = new HashMap<>();
                transitionMap.put("id", targetTransitionId);
                bodyMap.put("transition", transitionMap);

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);

                // 直接调用 Jira 官方的状态流转接口
                String transitionUrl = jiraUrl + "/rest/api/2/issue/" + issueKey + "/transitions";
                restTemplate.postForEntity(transitionUrl, requestEntity, String.class);

                System.out.println("✅ 状态更新成功！");
            } else {
                System.out.println("\n❌ 未找到目标状态，请检查上面打印出的状态名称，并在代码中调整匹配条件。");
            }

        } catch (Exception e) {
            System.err.println("❌ 更新状态失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

    @Test
    public void testPageDemand() {
        // ====================== 1. 填写你的 Jira 信息 ======================
        String jiraUrl = "https://fit2cloudtest.atlassian.net";
        String email = "yue.lu@fit2cloud.com";
        String apiToken = "ATATT3xFfGF0f2JnRRcT3M6Z0BpO-R1aK6wVJT_yiHMRHsqq93izp0XnnkvhWxrut2Fn3w_DtCxIAvc-lS4mND-CutT63dCVSo5ZmuPjgtkwxdm21kOCe4etOKROP9vst4NuUmNnc2SGpVb4ME4Co7pgHLIezHbNRfYKf0o45NXf56dHmxfUaHY=BF256997";

        // pageDemand 的核心查询参数
        String projectKey = "KAN";      // 你要查询的项目 Key
        String issueType = "Bug";       // 你要查询的缺陷类型 (如 Bug, Task, Story)
        int startAt = 0;                // 分页起始位置 (从第0条开始)
        int maxResults = 10;            // 每页返回的最大记录数
        String queryKeyword = "";       // 额外的模糊查询关键词 (例如输入具体的缺陷标题进行筛选)

        try {
            System.out.println("🔍 正在分页查询项目 [" + projectKey + "] 下的缺陷列表...");

            // ====================== 2. 自己在测试类里创建 RestTemplate 和 认证头 ======================
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();

            // POST 请求必须设置 Content-Type 为 JSON
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");

            // 手动拼接 Basic Auth 认证头
            String credentials = email + ":" + apiToken;
            String encodedCredentials = Base64.getEncoder().encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8)
            );
            headers.set("Authorization", "Basic " + encodedCredentials);

            // ====================== 3. 构建 pageDemand 的请求体 (JSON) ======================
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("startAt", startAt);
            requestBody.put("maxResults", maxResults);

            // 构造基础的 JQL 查询语句 (Jira Query Language)
            // 逻辑：项目等于 KAN 并且 缺陷类型等于 Bug
            StringBuilder jql = new StringBuilder();
            jql.append("project=").append(projectKey);
            jql.append(" AND issuetype=\"").append(issueType).append("\"");

            // 如果有额外的关键词，追加到 JQL 中
            if (queryKeyword != null && !queryKeyword.trim().isEmpty()) {
                jql.append(" AND summary ~ \"").append(queryKeyword).append("\"");
            }
            requestBody.put("jql", jql.toString());

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // ====================== 4. 直接调用 Jira 官方的 Search 接口 ======================
            // 官方 V3 搜索接口：/rest/api/3/search
            String apiUrl = jiraUrl + "/rest/api/3/search";

            // 发送 POST 请求，返回 String 类型的 JSON 字符串
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

            // ====================== 5. 处理响应结果 ======================
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ 分页查询成功！HTTP 状态码: " + response.getStatusCode());

                // 打印返回的原始 JSON，里面会包含 total(总数), issues(当前页的缺陷列表) 等字段
                System.out.println("📄 返回的 JSON 数据如下：");
                System.out.println(response.getBody());
            } else {
                System.err.println("❌ API 调用失败，HTTP 状态码: " + response.getStatusCode());
            }

        } catch (Exception e) {
            System.err.println("❌ 查询异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
*/

}
