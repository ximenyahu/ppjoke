package com.mooc.libnavcompiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.auto.service.AutoService;
import com.mooc.libnavannotation.ActivityDestination;
import com.mooc.libnavannotation.FragmentDestination;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"com.mooc.libnavannotation.FragmentDestination",
        "com.mooc.libnavannotation.ActivityDestination"})
public class NavProcessor extends AbstractProcessor {

    private Messager mMessager;
    private Filer mFiler;
    private static final String OUTPUT_FILE_NAME = "destination.json";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mMessager = processingEnvironment.getMessager();
        mFiler = processingEnvironment.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        Set<? extends Element> fragmentElements = roundEnvironment.getElementsAnnotatedWith(FragmentDestination.class);
        Set<? extends Element> activityElements = roundEnvironment.getElementsAnnotatedWith(ActivityDestination.class);
        if (!fragmentElements.isEmpty() || !activityElements.isEmpty()) {
            HashMap<String, JSONObject> destMap = new HashMap<>();
            handleDestination(fragmentElements, FragmentDestination.class, destMap);
            handleDestination(activityElements, ActivityDestination.class, destMap);

            //app/src/main/asserts/
            FileOutputStream fos = null;
            OutputStreamWriter writer = null;
            try {
                FileObject resource = mFiler.createResource(StandardLocation.CLASS_OUTPUT, "", OUTPUT_FILE_NAME);
                String resourcePath = resource.toUri().getPath();
                mMessager.printMessage(Diagnostic.Kind.NOTE, "message:" + resourcePath);
                String appPath = resourcePath.substring(0, resourcePath.indexOf("app") + 4);
                String assertPath = appPath + "src/main/asserts/";

                File file = new File(assertPath);

                if (!file.exists()) {
                    file.mkdirs();
                }

                File outPutFile = new File(file, OUTPUT_FILE_NAME);
                if (outPutFile.exists()) {
                    outPutFile.delete();
                }
                outPutFile.createNewFile();
                String content = JSON.toJSONString(destMap);
                fos = new FileOutputStream(outPutFile);
                writer = new OutputStreamWriter(fos, "UTF-8");
                writer.write(content);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
    }


    private void handleDestination(Set<? extends Element> elements, Class<? extends Annotation> annotationClass, HashMap<String, JSONObject> destMap) {
        for (Element element : elements) {
            TypeElement typeElement = (TypeElement) element;
            String pageUrl = null;
            String className = typeElement.getQualifiedName().toString();
            int id = Math.abs(className.hashCode());
            boolean needLogin = false;
            boolean asStarter = false;
            boolean isFragment = false;

            Annotation annotation = typeElement.getAnnotation(annotationClass);
            if (annotation instanceof FragmentDestination) {
                FragmentDestination fragmentDestination = (FragmentDestination) annotation;
                pageUrl = fragmentDestination.pageUrl();
                needLogin = fragmentDestination.needLogin();
                asStarter = fragmentDestination.asStarter();
                isFragment = true;
            } else if (annotation instanceof ActivityDestination) {
                ActivityDestination activityDestination = (ActivityDestination) annotation;
                pageUrl = activityDestination.pageUrl();
                needLogin = activityDestination.needLogin();
                asStarter = activityDestination.asStarter();
                isFragment = false;
            }

            if (destMap.containsKey(pageUrl)) {
                mMessager.printMessage(Diagnostic.Kind.ERROR, "不同的页面不允许使用相同的pageUrl" + className);
            } else {
                JSONObject object = new JSONObject();
                object.put("pageUrl", pageUrl);
                object.put("className", className);
                object.put("id", id);
                object.put("isFragment", isFragment);
                object.put("asStarter", asStarter);
                object.put("needLogin", needLogin);
                destMap.put(pageUrl, object);
            }
        }
    }
}
