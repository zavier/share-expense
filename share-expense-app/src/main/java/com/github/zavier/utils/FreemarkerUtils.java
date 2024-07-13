package com.github.zavier.utils;

import com.alibaba.cola.exception.BizException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

@Slf4j
public class FreemarkerUtils {

    public static String processTemplate(String templateName, Map<String, Object> data) {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassForTemplateLoading(FreemarkerUtils.class, "/template");

        try {
            Template template = cfg.getTemplate(templateName);
            StringWriter stringWriter = new StringWriter();
            template.process(data, stringWriter);
            return stringWriter.toString();
        } catch (IOException | TemplateException e) {
            log.error("freemarker处理异常", e);
            throw new BizException("freemarker处理异常");
        }
    }

}
