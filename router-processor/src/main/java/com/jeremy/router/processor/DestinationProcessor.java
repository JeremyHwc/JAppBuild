package com.jeremy.router.processor;

import com.google.auto.service.AutoService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jeremy.router.annotations.Destination;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

@AutoService(Processor.class)
public class DestinationProcessor extends AbstractProcessor {
    private static final String TAG = "DestinationProcessor";

    /**
     * 编译器找到我们关心的注解后，会回调该方法
     *
     * @param set
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        // 如果已经处理过，直接返回,避免多次调用process
        if (roundEnvironment.processingOver()) {
            return false;
        }

        System.out.println(TAG + " >>> process start ...");

        // 从RoundEnvironment获取被Destination注解的元素
        Set<? extends Element> allDestinationElements = roundEnvironment.getElementsAnnotatedWith(Destination.class);
        System.out.println(TAG + " >>> all Destination elements count = " + allDestinationElements.size());
        if (allDestinationElements.size() < 1) { // 当未收集到@Destination注解的时候，跳过后续流程
            return false;
        }

        // 将要自动生成的类的类名
        String className = "RouterMapping_" + System.currentTimeMillis();
        StringBuilder builder = new StringBuilder();
        builder.append("package com.jeremy.jappbuild.mapping;\n\n")
                .append("import java.util.HashMap;\n")
                .append("import java.util.Map;\n\n")
                .append("public class " + className + " {\n")
                .append("    public static Map<String, String> get() {\n")
                .append("        Map<String, String> mapping = new HashMap<>();\n");

        final JsonArray destinationJsonArr = new JsonArray();

        // 遍历所有 @Destination 注解信息，挨个获取详细信息
        for (Element element : allDestinationElements) {
            final TypeElement typeElement = (TypeElement) element;
            // 尝试在当前类上，获取@Destination的信息
            final Destination destination = typeElement.getAnnotation(Destination.class);
            if (destination == null) {
                continue;
            }
            String url = destination.url();
            String description = destination.description();
            String realPath = typeElement.getQualifiedName().toString();

            builder.append("        mapping.put(\"")
                    .append(url)
                    .append("\", \"")
                    .append(realPath)
                    .append("\");\n");

            System.out.println(TAG + " >>> url = " + url + " >>> description = " + description + " >>> realPath = " + realPath);

            // 遍历完成以后，destinationJsonArr就拥有了子工程所有页面的信息的json字符串信息
            JsonObject item = new JsonObject();
            item.addProperty("url", url);
            item.addProperty("description", description);
            item.addProperty("realPath", realPath);
            destinationJsonArr.add(item);
        }

        builder.append("        return mapping;\n")
                .append("    }\n")
                .append("}");

        // 将builder字符串写入文件
        String mappingFullClassName = "com.jeremy.jappbuild.mapping." + className;
        System.out.println(TAG + " >>> mappingFullClassName = " + mappingFullClassName);
        System.out.println(TAG + " >>> class content = \n" + builder.toString());
        // 自动生成类映射文件
        flushToFile(mappingFullClassName, builder.toString());
        // 自动生成文档的json信息
        flushJsonToFile(destinationJsonArr.toString());

        System.out.println(TAG + " >>> process finish ...");

        return false;
    }

    private void flushJsonToFile(String content) {
        String rootProjectDir = processingEnv.getOptions().get("root_project_dir");
        File rootProjectFile = new File(rootProjectDir);
        if (!rootProjectFile.exists()) {
            throw new RuntimeException("root_project_dir not exist");
        }
        // 创建router_mapping子目录
        File routerFileDir = new File(rootProjectFile, "router_mapping");
        if (!routerFileDir.exists()) {
            routerFileDir.mkdirs();
        }
        File mappingFile = new File(routerFileDir, "mapping_" + System.currentTimeMillis() + ".json");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(mappingFile))) {
            writer.write(content);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Error while writing json", e);
        }
    }


    private void flushToFile(String mappingFullClassName, String content) {
        try {
            JavaFileObject source = processingEnv.getFiler().createSourceFile(mappingFullClassName);
            Writer writer = source.openWriter();
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Error while create file ", e);
        }
    }

    private void t() {
        // 获取外部声明的参数
        String rootDir = processingEnv.getOptions().get("root_project_dir");
    }

    // 告诉编译器当前处理器支持的注解类型
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Destination.class.getCanonicalName());
    }
}
