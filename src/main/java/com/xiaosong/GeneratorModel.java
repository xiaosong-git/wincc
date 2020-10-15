package com.xiaosong;

import com.jfinal.kit.PathKit;
import com.jfinal.kit.Prop;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.dialect.MysqlDialect;
import com.jfinal.plugin.activerecord.generator.Generator;
import com.jfinal.plugin.druid.DruidPlugin;

import javax.sql.DataSource;

public class GeneratorModel {

    private static DataSource getDataSource() {
        Prop p = PropKit.use("db_product.properties");
        DruidPlugin druidPlugin=new DruidPlugin(p.get("jdbcUrl"), p.get("user"), p.get("password"));
        druidPlugin.start();
        return druidPlugin.getDataSource();
    }

    public static void main(String[] args) {
        // base model 实体类所使用的包名
        String baseModelPackageName = "com.xiaosong.model.base";

        // base model 文件保存路径
        String baseModelOutputDir = PathKit.getWebRootPath() + "/src/main/java/com/xiaosong/wincc/model/base";
        System.out.println(baseModelOutputDir);
        // model 所使用的包名 (MappingKit 默认使用的包名)
        String modelPackageName = "com.xiaosong.model";
        // model 文件保存路径 (MappingKit 与 DataDictionary 文件默认保存路径)
        String modelOutputDir = baseModelOutputDir + "/..";
        System.out.println(modelOutputDir);

        // 创建生成器
        Generator gernerator = new Generator(getDataSource(), baseModelPackageName, baseModelOutputDir, modelPackageName, modelOutputDir);
        // 设置数据库方言
        gernerator.setDialect(new MysqlDialect());
        // 设置是否在 Model 中生成 dao 对象
        gernerator.setGenerateDaoInModel(true);
        //链式写法
        gernerator.setGenerateChainSetter(true);
        // 设置是否生成字典文件
        gernerator.setGenerateDataDictionary(false);
        // 生成
        gernerator.generate();
    }
}
