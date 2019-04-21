package com.finalproject.automated.refactoring.tool.detection.service;

import com.finalproject.automated.refactoring.tool.model.MethodModel;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;

/**
 * @author Faza Zulfika P P
 * @version !.0.0
 * @since 21 April 2019
 */

public interface Detection {

    Map<String, List<MethodModel>> detect(@NonNull String path);

    Map<String, List<MethodModel>> detect(@NonNull List<String> paths);
}
