package com.example.backend.config;

import com.example.backend.entity.Content;
import com.example.backend.entity.User;
import com.example.backend.entity.UserBehavior;
import com.example.backend.repository.ContentRepository;
import com.example.backend.repository.UserBehaviorRepository;
import com.example.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 演示数据初始化。
 *
 * 只在对应表为空时执行，不会覆盖已有数据，
 * 保证第一次启动就能看到完整的推荐效果。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;

    private final ContentRepository contentRepository;

    private final UserBehaviorRepository userBehaviorRepository;

    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            ContentRepository contentRepository,
            UserBehaviorRepository userBehaviorRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.userBehaviorRepository = userBehaviorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        List<User> users = seedUsers();

        List<Content> contents = seedContents(users);

        seedBehaviors(users, contents);
    }

    /* ============================
       用户
    ============================ */

    private List<User> seedUsers() {

        if (userRepository.count() > 0) {
            return userRepository.findAll();
        }

        List<User> users = new ArrayList<>();

        users.add(createUser(
                "admin",
                "admin123",
                "管理员",
                "admin@example.com",
                "ADMIN"
        ));

        users.add(createUser(
                "demo",
                "demo123",
                "体验用户",
                "demo@example.com",
                "USER"
        ));

        users.add(createUser(
                "lily",
                "lily123",
                "Lily",
                "lily@example.com",
                "USER"
        ));

        users.add(createUser(
                "ken",
                "ken123",
                "Ken",
                "ken@example.com",
                "USER"
        ));

        log.info("已初始化 {} 个演示账号（admin/admin123，demo/demo123）", users.size());

        return users;
    }

    private User createUser(
            String username,
            String rawPassword,
            String nickname,
            String email,
            String role
    ) {

        User user = new User();

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setRole(role);

        LocalDateTime now = LocalDateTime.now();

        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return userRepository.save(user);
    }

    /* ============================
       内容
    ============================ */

    private List<Content> seedContents(List<User> users) {

        if (contentRepository.count() > 0) {
            return contentRepository.findAll();
        }

        if (users.isEmpty()) {
            return List.of();
        }

        Long authorA = users.get(0).getId();
        Long authorB = users.get(1 % users.size()).getId();
        Long authorC = users.get(2 % users.size()).getId();

        List<Content> seeds = new ArrayList<>();

        seeds.add(build("大模型推理成本一年下降 90%，这意味着什么",
                "科技",
                "过去一年，主流大模型的单位推理成本下降了接近一个数量级。这不是简单的工程优化，而是量化、蒸馏、稀疏化和硬件协同四件事同时发生的结果。成本曲线的陡峭下滑，正在把很多“用不起”的场景变成“值得一试”。",
                authorA, 1820, 246, 2));

        seeds.add(build("从 RAG 到 Agent：检索增强的下一站",
                "科技",
                "RAG 解决的是“模型不知道”的问题，而 Agent 要解决的是“模型做不到”的问题。当检索从一次性上下文注入，演化为可循环调用的工具，系统的复杂度重心就从提示词转移到了编排与容错上。",
                authorB, 1460, 198, 4));

        seeds.add(build("向量数据库选型：Milvus、Qdrant 与 pgvector 实测",
                "科技",
                "在千万级向量的量级下，三者的召回率差距并不大，真正的分水岭出现在运维成本和生态耦合度上。如果你的数据本来就在 Postgres 里，pgvector 的边际收益往往被严重低估。",
                authorC, 980, 132, 6));

        seeds.add(build("端侧 AI 的临界点：为什么手机厂商都在抢 NPU",
                "科技",
                "隐私、延迟、成本，三股力量把推理推向端侧。当 7B 模型能在手机上以 20 token/s 运行时，产品形态的可能性就不再受网络条件约束了。",
                authorA, 1240, 176, 9));

        seeds.add(build("玻璃拟态回潮：从界面质感的十年轮回看设计趋势",
                "设计",
                "从拟物到扁平，再到今天的半透明玻璃，界面风格始终在“真实感”和“信息密度”之间摆动。玻璃拟态之所以回归，是因为它同时提供了层级感和轻盈感——而这恰好是移动端最稀缺的两种表达。",
                authorB, 2130, 312, 1));

        seeds.add(build("留白的成本：为什么克制比堆料更难",
                "设计",
                "删掉一个元素，需要说服的人比加上一个元素多得多。留白的真正成本不是空间，而是决策——你必须明确知道什么最重要。",
                authorC, 1560, 224, 5));

        seeds.add(build("设计系统落地失败的五个真实原因",
                "设计",
                "大多数设计系统不是死于设计，而是死于治理。没有明确的负责人、没有强制的接入流程、没有版本策略，组件库很快就会变成第二个“废弃项目目录”。",
                authorA, 1120, 158, 8));

        seeds.add(build("暗色模式不是把颜色反过来这么简单",
                "设计",
                "直接反色会破坏层级关系：原本靠阴影区分的前后景，在深色背景上会彻底塌陷。正确的做法是用明度而非亮度重建层级，同时降低大面积纯色块的饱和度。",
                authorB, 890, 121, 12));

        seeds.add(build("订阅制陷阱：留存比增长更重要",
                "商业",
                "订阅模式的财务美感来自复利，而复利的前提是留存。当获客成本高于 12 个月的用户生命周期价值时，增长越快，现金流失血越快。",
                authorC, 1340, 189, 3));

        seeds.add(build("一家小公司如何用 12 人做到 2000 万 ARR",
                "商业",
                "他们做对的事情只有一件：把产品边界收窄到极致。12 个人服务 400 家企业客户，靠的不是效率工具，而是拒绝掉 90% 的需求。",
                authorA, 1980, 267, 7));

        seeds.add(build("平台补贴战的终局：网络效应还是现金流",
                "商业",
                "补贴能买到规模，但买不到网络效应。如果用户迁移成本没有随着规模上升，补贴停止的那一刻，增长曲线就会立刻掉头。",
                authorB, 1050, 143, 11));

        seeds.add(build("出海第一年，我们踩过的合规坑",
                "商业",
                "数据本地化、税务登记、用户协议的可执行性——这些在国内看起来是“流程问题”的事情，在海外往往是产品能否上架的前置条件。",
                authorC, 760, 98, 15));

        seeds.add(build("技术管理者最容易犯的三个错",
                "职场",
                "第一是抢着写代码，第二是用技术标准评价所有人，第三是把“我来做更快”当成效率。管理的产出是团队产出，不是个人产出。",
                authorA, 1670, 231, 2));

        seeds.add(build("如何写出让老板一眼看懂的周报",
                "职场",
                "周报不是流水账，而是决策材料。先写结论，再写依据，最后才写过程。如果老板需要读完三屏才知道你在做什么，这份周报就是失败的。",
                authorB, 1420, 203, 6));

        seeds.add(build("面试官视角：什么样的候选人会被记住",
                "职场",
                "不是答对最多题的人，而是能清楚说明“我为什么这么做”的人。技术决策背后的权衡，比技术本身更能体现水平。",
                authorC, 1180, 167, 10));

        seeds.add(build("从执行者到负责人，中间隔着什么",
                "职场",
                "隔着“不确定性”。执行者的任务是消除模糊，负责人的任务是拥抱模糊并给出方向。这个转变的难点不在能力，而在心态。",
                authorA, 940, 129, 14));

        seeds.add(build("每天 20 分钟的复利：我坚持三年的阅读习惯",
                "生活",
                "20 分钟看起来微不足道，但三年下来是 365 小时。习惯的关键从来不是强度，而是不可协商的固定时段。",
                authorB, 1760, 248, 4));

        seeds.add(build("断舍离的本质是决策成本管理",
                "生活",
                "物品越多，每次选择消耗的注意力越多。整理的目的不是让家变空，而是降低日常生活里的决策负担。",
                authorC, 1230, 174, 9));

        seeds.add(build("一个人住的第五年，我学会了做饭",
                "生活",
                "做饭真正的门槛不是厨艺，而是接受“一顿饭只需要二十分钟”这个事实。把标准降到可执行的程度，习惯才可能持续。",
                authorA, 1010, 145, 13));

        seeds.add(build("通勤路上适合听的五档播客",
                "生活",
                "通勤时间的特点是碎片且不可控，适合信息密度适中、不依赖视觉的内容。访谈类比叙事类更适合被打断。",
                authorB, 870, 112, 17));

        seeds.add(build("《思考，快与慢》重读笔记：系统一与系统二",
                "阅读",
                "系统一快速但易受偏见影响，系统二理性但懒惰。大部分认知偏差的产生，是因为系统二在关键时刻缺席。",
                authorC, 1490, 214, 5));

        seeds.add(build("为什么我们读不完经典",
                "阅读",
                "经典的阅读门槛不在于语言，而在于背景知识。缺少语境时，读者无法判断作者在回应什么，自然就读不出趣味。",
                authorA, 1080, 152, 12));

        seeds.add(build("笔记方法的进化：从摘抄到双向链接",
                "阅读",
                "摘抄是知识的搬运，链接才是知识的再生产。真正的价值不在于记录了多少，而在于两条笔记之间建立了多少次意外的连接。",
                authorB, 1320, 193, 16));

        seeds.add(build("一年读 100 本书之后，我放弃了读书计划",
                "阅读",
                "数量目标会诱导你选择更薄的书和更浅的读法。放弃计划之后，阅读反而变成了真正需要时才发生的深度行为。",
                authorC, 950, 136, 20));

        return contentRepository.saveAll(seeds);
    }

    private Content build(
            String title,
            String category,
            String content,
            Long authorId,
            int viewCount,
            int likeCount,
            int daysAgo
    ) {

        Content entity = new Content();

        entity.setTitle(title);
        entity.setCategory(category);
        entity.setContent(content);
        entity.setAuthorId(authorId);
        entity.setViewCount(viewCount);
        entity.setLikeCount(likeCount);

        LocalDateTime created = LocalDateTime.now()
                .minusDays(daysAgo)
                .minusHours(daysAgo % 7);

        entity.setCreatedAt(created);
        entity.setUpdatedAt(created);

        return entity;
    }

    /* ============================
       行为
    ============================ */

    /**
     * 给体验账号灌入浏览与点赞记录，让个性化推荐一开始就有信号。
     */
    private void seedBehaviors(List<User> users, List<Content> contents) {

        if (userBehaviorRepository.count() > 0) {
            return;
        }

        if (users.size() < 2 || contents.isEmpty()) {
            return;
        }

        User demo = users.stream()
                .filter(user -> "demo".equals(user.getUsername()))
                .findFirst()
                .orElse(users.get(1));

        List<UserBehavior> behaviors = new ArrayList<>();

        // 体验用户对「科技」和「设计」表现出明显偏好
        String[] preferred = {"科技", "设计", "职场"};

        int liked = 0;

        for (Content content : contents) {

            boolean isPreferred = false;

            for (String category : preferred) {
                if (category.equals(content.getCategory())) {
                    isPreferred = true;
                    break;
                }
            }

            if (!isPreferred) {
                continue;
            }

            behaviors.add(behavior(
                    demo.getId(),
                    content.getId(),
                    "VIEW"
            ));

            // 偏好分类里的高赞内容产生点赞行为
            if (content.getLikeCount() != null
                    && content.getLikeCount() >= 180
                    && liked < 5) {

                behaviors.add(behavior(
                        demo.getId(),
                        content.getId(),
                        "LIKE"
                ));

                liked++;
            }
        }

        userBehaviorRepository.saveAll(behaviors);

        log.info(
                "已初始化 {} 条体验用户行为记录（浏览/点赞），个性化推荐可直接生效",
                behaviors.size()
        );
    }

    private UserBehavior behavior(
            Long userId,
            Long contentId,
            String type
    ) {

        UserBehavior behavior = new UserBehavior();

        behavior.setUserId(userId);
        behavior.setContentId(contentId);
        behavior.setBehaviorType(type);

        return behavior;
    }
}
