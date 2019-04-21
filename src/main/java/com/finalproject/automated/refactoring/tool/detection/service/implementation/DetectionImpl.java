package com.finalproject.automated.refactoring.tool.detection.service.implementation;

import com.finalproject.automated.refactoring.tool.code.smells.detection.service.CodeSmellsDetection;
import com.finalproject.automated.refactoring.tool.detection.service.Detection;
import com.finalproject.automated.refactoring.tool.files.detection.model.FileModel;
import com.finalproject.automated.refactoring.tool.files.detection.service.FilesDetection;
import com.finalproject.automated.refactoring.tool.methods.detection.service.MethodsDetection;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Faza Zulfika P P
 * @version 1.0.0
 * @since 21 April 2019
 */

@Service
public class DetectionImpl implements Detection {

    @Autowired
    private FilesDetection filesDetection;

    @Autowired
    private MethodsDetection methodsDetection;

    @Autowired
    private CodeSmellsDetection codeSmellsDetection;

    @Value("${files.mime.type}")
    private String mimeType;

    @Override
    public Map<String, List<MethodModel>> detect(String path) {
        return detect(Collections.singletonList(path));
    }

    @Override
    public Map<String, List<MethodModel>> detect(List<String> paths) {
        return filesDetection.detect(paths, mimeType)
                .values()
                .parallelStream()
                .map(this::detectMethods)
                .map(Map::entrySet)
                .flatMap(Collection::parallelStream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, this::mergeList));
    }

    private Map<String, List<MethodModel>> detectMethods(List<FileModel> fileModels) {
        Map<String, List<MethodModel>> methods = methodsDetection.detect(fileModels);
        methods.forEach(this::detectCodeSmells);

        return methods;
    }

    private void detectCodeSmells(String filename, List<MethodModel> methods) {
        codeSmellsDetection.detect(methods);
    }

    private List<MethodModel> mergeList(List<MethodModel> methodModels, List<MethodModel> nextMethodModels) {
        methodModels.addAll(nextMethodModels);
        return methodModels;
    }
}
