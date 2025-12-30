import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.dictionary.CustomDictionary;

import java.util.List;

/**
 * HanLP 缓存文件生成工具
 * 
 * 用途：预先生成 HanLP 的二进制缓存文件（.bin），避免运行时首次加载耗时和权限问题
 * 
 * 使用方法：
 * 1. cd backend/infra
 * 2. mvn compile
 * 3. mvn exec:java -Dexec.mainClass="HanLPCacheGenerator"
 * 
 * 或者直接运行：
 * java -cp "target/classes:~/.m2/repository/com/hankcs/hanlp/portable-1.8.4.jar" HanLPCacheGenerator
 */
public class HanLPCacheGenerator {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("HanLP 缓存文件生成工具");
        System.out.println("========================================\n");
        
        try {
            // 1. 加载核心词典（会自动生成 .bin 缓存）
            System.out.println("[1/6] 加载核心词典...");
            List<Term> terms = HanLP.segment("测试文本");
            System.out.println("✓ 核心词典加载成功");
            
            // 2. 加载自定义词典
            System.out.println("\n[2/6] 加载自定义词典...");
            CustomDictionary.insert("测试词", "n 1000");
            System.out.println("✓ 自定义词典加载成功");
            
            // 3. 加载简繁体转换字典
            System.out.println("\n[3/6] 加载简繁体转换字典...");
            String simplified = "简体中文测试";
            String traditional = HanLP.convertToTraditionalChinese(simplified);
            System.out.println("  简体: " + simplified);
            System.out.println("  繁体: " + traditional);
            
            String backToSimplified = HanLP.convertToSimplifiedChinese(traditional);
            System.out.println("  转回: " + backToSimplified);
            System.out.println("✓ 简繁体转换字典加载成功");
            
            // 4. 加载拼音转换
            System.out.println("\n[4/6] 加载拼音字典...");
            List<String> pinyinList = HanLP.convertToPinyinList("中国");
            System.out.println("  中国 -> " + pinyinList);
            System.out.println("✓ 拼音字典加载成功");
            
            // 5. 加载关键词提取模型
            System.out.println("\n[5/6] 加载关键词提取模型...");
            List<String> keywords = HanLP.extractKeyword("HanLP是一款优秀的自然语言处理工具", 3);
            System.out.println("  关键词: " + keywords);
            System.out.println("✓ 关键词提取模型加载成功");
            
            // 6. 加载命名实体识别模型
            System.out.println("\n[6/6] 加载命名实体识别模型...");
            List<Term> nerTerms = HanLP.segment("我在北京天安门广场");
            for (Term term : nerTerms) {
                if (term.nature.startsWith("ns")) {  // 地名
                    System.out.println("  地名: " + term.word);
                }
            }
            System.out.println("✓ 命名实体识别模型加载成功");
            
            // 完成
            System.out.println("\n========================================");
            System.out.println("✓ 所有缓存文件生成完成！");
            System.out.println("========================================");
            
            // 显示缓存文件位置
            System.out.println("\n缓存文件位置：");
            System.out.println("  - 核心词典: data/dictionary/CoreNatureDictionary.txt.bin");
            System.out.println("  - 二元词典: data/dictionary/CoreNatureDictionary.ngram.txt.bin");
            System.out.println("  - 简转繁: data/dictionary/tc/s2t.txt.bin");
            System.out.println("  - 繁转简: data/dictionary/tc/t2s.txt.bin");
            System.out.println("  - 其他模型缓存文件");
            
        } catch (Exception e) {
            System.err.println("\n✗ 缓存生成失败！");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
