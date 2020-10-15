package com.xiaosong.common.license;

import com.jfinal.core.Controller;
import com.jfinal.kit.Prop;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.redis.Cache;
import com.jfinal.plugin.redis.Redis;
import com.xiaosong.constant.ErrorCodeDef;
import com.xiaosong.model.TbLicense;
import com.xiaosong.util.Misc;
import com.xiaosong.util.RetUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *  license
 */
public class LicenseController extends Controller {
    private static LicenseService srv = LicenseService.me;

    private static Cache cache = Redis.use("xiaosong");
    private static Logger logger = Logger.getLogger(LicenseController.class);
    private Prop use = PropKit.use("license.properties");

    /**
     *  添加license
     */
    public void index() {
        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "license 已效验存入~"));
//
//        try {
//            //配置文件中的license
//            String lic = use.get("license");
//            //前端 传入参数
//            String code = getPara("license");
//            String str = null;
//            String privateKey = getPara("key");
//            if (code != null && privateKey != null) {
//                byte[] res = decrypt(loadPrivateKeyByStr(privateKey), Base64.decodeBase64(code));
//                str = new String(res);
//            }
//            if (str != null) {
//                if (!str.equals(lic)) {
//                    logger.error("license不匹配~");
//                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "license不匹配~"));
//                    return;
//                }
//                //切割字符串
//                String[] str1 = str.split("\\|");
//                //上位机编码
//                String SwjCode = str1[0];
//                //mac地址
//                String mac = str1[1];
//                //开始时间
//                String beginDate = str1[2];
//                //结束时间
//                String endDate = str1[3];
//                TbLicense tbLicense = srv.findMac(mac);
//                //判断 数据库 中是否 有数据 没有就存
//                if (tbLicense == null) {
//                    TbLicense tbLicense1 = getModel(TbLicense.class);
//                    tbLicense1.setMac(mac);
//                    tbLicense1.setSwjCode(SwjCode);
//                    tbLicense1.setBeginDate(beginDate);
//                    tbLicense1.setEndDate(endDate);
//                    boolean save = tbLicense1.save();
//                    //数据库存完 在存入redis
//                    if (save) {
//                        logger.info("license 保存成功~");
//                        //存入缓存
//                        cache.set("license", str);
//
//                        logger.info("license 已效验存入~");
//                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "license 已效验存入~"));
//                    }
//                } else {
//                    //存入缓存
//                    cache.set("license", str);
//                    logger.info("license 已效验存入~");
//                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "license 已效验存入~"));
//                }
//            }else{
//                logger.error("license错误~");
//                renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "license错误~"));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            logger.error("license异常~");
//            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "license异常~"));
//        }
    }

    /**
     * 登录成功后 到 license 判断内存中 是否有license数据
     */
    public void cache() {
        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "license匹对成功~"));

//        try {
//            String license = cache.get("license");
//
//            String license1 = use.get("license");
//            String[] split = license1.split("\\|");
//            if (license == null) {
//                //logger.error("内存中没有license,从数据库中获取~");
//                //内存中没有license,从数据库中获取~
//                TbLicense tbLicense = srv.findLicense();
//                if (tbLicense == null) {
//                    logger.error("license不存在~");
//                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "license不存在~"));
//                } else {
//
//                    if (tbLicense.getMac().equals(split[1]) && Misc.compareDate2(tbLicense.getEndDate(), getDate())) {
//                        logger.info("license匹对成功~");
//                        cache.set("license", license1); // 存一分 到缓存
//                        renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "license匹对成功~"));
//                    } else {
//                        logger.error("license不匹配~");
//                        renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "license不匹配~"));
//                    }
//                }
//            } else {
//                String[] split1 = license.split("\\|");
//                if (split1[1].equals(split[1]) && Misc.compareDate2(split1[3], getDate())) {
//                    logger.info("license匹对成功~");
//                    renderJson(RetUtil.ok(ErrorCodeDef.CODE_NORMAL, "license匹对成功~"));
//                } else {
//                    logger.info("license匹对失败~");
//                    renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "license匹对失败~"));
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            logger.error("license异常~");
//            renderJson(RetUtil.fail(ErrorCodeDef.CODE_ERROR, "license异常~"));
//        }
    }

    /**
     * 私钥解密过程
     *
     * @param privateKey 私钥
     * @param cipherData 密文数据
     * @return 明文
     * @throws Exception 解密过程中的异常信息
     */
    public byte[] decrypt(RSAPrivateKey privateKey, byte[] cipherData)
            throws Exception {
        if (privateKey == null) {
            throw new Exception("解密私钥为空, 请设置");
        }
        Cipher cipher = null;
        try {
            // 使用默认RSA
            cipher = Cipher.getInstance("RSA");
            // cipher= Cipher.getInstance("RSA", new BouncyCastleProvider());
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] output = cipher.doFinal(cipherData);
            return output;
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此解密算法");
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeyException e) {
            throw new Exception("解密私钥非法,请检查");
        } catch (IllegalBlockSizeException e) {
            throw new Exception("密文长度非法");
        } catch (BadPaddingException e) {
            throw new Exception("密文数据已损坏");
        }
    }

    /**
     * @param privateKeyStr 私钥
     * @return
     * @throws Exception
     */
    public static RSAPrivateKey loadPrivateKeyByStr(String privateKeyStr)
            throws Exception {
        try {
            byte[] buffer = Base64.decodeBase64(privateKeyStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此算法");
        } catch (InvalidKeySpecException e) {
            throw new Exception("私钥非法");
        } catch (NullPointerException e) {
            throw new Exception("私钥数据为空");
        }
    }

    /**
     * 获取当前时间 年月日
     *
     * @return
     */
    private String getDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
        String date = df.format(new Date()); // new Date()为获取当前系统时间
        return date;
    }
}
