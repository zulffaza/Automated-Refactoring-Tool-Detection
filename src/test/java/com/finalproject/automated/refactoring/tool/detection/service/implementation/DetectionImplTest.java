package com.finalproject.automated.refactoring.tool.detection.service.implementation;

import com.finalproject.automated.refactoring.tool.code.smells.detection.service.CodeSmellsDetection;
import com.finalproject.automated.refactoring.tool.detection.service.Detection;
import com.finalproject.automated.refactoring.tool.files.detection.model.FileModel;
import com.finalproject.automated.refactoring.tool.files.detection.service.FilesDetection;
import com.finalproject.automated.refactoring.tool.methods.detection.service.MethodsDetection;
import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.PropertyModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DetectionImplTest {

    @Autowired
    private Detection detection;

    @MockBean
    private FilesDetection filesDetection;

    @MockBean
    private MethodsDetection methodsDetection;

    @MockBean
    private CodeSmellsDetection codeSmellsDetection;

    @Value("${files.mime.type}")
    private String mimeType;

    private static final Integer FIRST_INDEX = 0;
    private static final Integer INVOKED_ONCE = 1;

    private static final String FILENAME = "Filename.java";

    private List<String> paths;

    @Before
    public void setUp() {
        paths = Collections.singletonList("path");

        when(filesDetection.detect(eq(paths), eq(mimeType)))
                .thenReturn(createFilesDetectionReturn());
        when(methodsDetection.detect(eq(createFileModels())))
                .thenReturn(createMethodsDetectionReturn());
        doNothing().when(codeSmellsDetection)
                .detect(eq(createMethodModels()));
    }

    @Test
    public void detect_singlePath_success() {
        Map<String, List<MethodModel>> result = detection.detect(paths.get(FIRST_INDEX));
        assertEquals(createExpectedResult().get(paths.get(FIRST_INDEX)), result);

        verifyFilesDetection();
        verifyMethodsDetection();
        verifyCodeSmellsDetection();
    }

    @Test
    public void detect_multiPath_success() {
        Map<String, Map<String, List<MethodModel>>> result = detection.detect(paths);
        assertEquals(createExpectedResult(), result);

        verifyFilesDetection();
        verifyMethodsDetection();
        verifyCodeSmellsDetection();
    }

    @Test(expected = NullPointerException.class)
    public void detect_singlePath_failed_pathIsNull() {
        String path = null;
        detection.detect(path);
    }

    @Test(expected = NullPointerException.class)
    public void detect_multiPath_failed_pathIsNull() {
        List<String> paths = null;
        detection.detect(paths);
    }

    private Map<String, List<FileModel>> createFilesDetectionReturn() {
        Map<String, List<FileModel>> result = new HashMap<>();
        result.put(paths.get(FIRST_INDEX), createFileModels());

        return result;
    }

    private List<FileModel> createFileModels() {
        return Collections.singletonList(createFileModel());
    }

    private FileModel createFileModel() {
        return FileModel.builder()
                .path(paths.get(FIRST_INDEX))
                .filename(FILENAME)
                .content(createFileContent())
                .build();
    }

    private static String createFileContent() {
        return "package path;\n" +
                "\n" +
                "import java.io.Serializable;\n" +
                "\n" +
                "public class Filename implements Serializable {\n" +
                "\n" +
                "    @GetMapping(\n" +
                "               value = \"/{filename}/change\",\n" +
                "               produces = MediaType.APPLICATION_JSON_VALUE\n" +
                "    )\n" +
                "    @SuppressWarnings()\n" +
                "    public Response<String, String> changeFilename(@RequestParam(required = false, defaultValue = \"null\") String name,\n" +
                "                                              @RequestParam(required = false, defaultValue = \".java\") String extension,\n" +
                "                                              @RequestParam(required = false) String user) throws Exception, IOException {\n" +
                "        try {\n" +
                "            return user + \"-\" + name + extension;\n" +
                "        } catch (NullPointerException e) {\n" +
                "            return null;\n" +
                "        }\n" +
                "    }\n" +
                "}";
    }

    private Map<String, List<MethodModel>> createMethodsDetectionReturn() {
        String key = paths.get(FIRST_INDEX) + File.separator + FILENAME;

        Map<String, List<MethodModel>> result = new HashMap<>();
        result.put(key, createMethodModels());

        return result;
    }

    private List<MethodModel> createMethodModels() {
        return Collections.singletonList(createMethodModel());
    }

    private MethodModel createMethodModel() {
        return MethodModel.builder()
                .keywords(Arrays.asList(
                        "@GetMapping( value = \"/{filename}/change\", produces = MediaType.APPLICATION_JSON_VALUE )",
                        "@SuppressWarnings()",
                        "public"))
                .returnType("Response<String, String>")
                .name("changeFilename")
                .parameters(Arrays.asList(
                        PropertyModel.builder()
                                .keywords(Collections.singletonList("@RequestParam(required = false, defaultValue = \"null\")"))
                                .type("String")
                                .name("name")
                                .build(),
                        PropertyModel.builder()
                                .keywords(Collections.singletonList("@RequestParam(required = false, defaultValue = \".java\")"))
                                .type("String")
                                .name("extension")
                                .build(),
                        PropertyModel.builder()
                                .keywords(Collections.singletonList("@RequestParam(required = false)"))
                                .type("String")
                                .name("user")
                                .build()))
                .exceptions(Arrays.asList("Exception", "IOException"))
                .statements(createExpectedStatements())
                .build();
    }

    private List<StatementModel> createExpectedStatements() {
        List<StatementModel> statements = new ArrayList<>();

        statements.add(createFirstStatement());
        statements.add(createSecondStatement());

        return statements;
    }

    private StatementModel createFirstStatement() {
        BlockModel blockModel = BlockModel.blockBuilder()
                .build();
        blockModel.setStatement("try {");
        blockModel.setStartIndex(9);
        blockModel.setEndIndex(13);
        blockModel.getStatements()
                .add(createFirstBlockStatement());
        blockModel.setEndOfBlockStatement(createFirstBlockEndStatement());

        return blockModel;
    }

    private StatementModel createFirstBlockStatement() {
        return StatementModel.statementBuilder()
                .statement("return user + \"-\" + name + extension;")
                .startIndex(27)
                .endIndex(63)
                .build();
    }

    private StatementModel createFirstBlockEndStatement() {
        return StatementModel.statementBuilder()
                .statement("}")
                .startIndex(73)
                .endIndex(73)
                .build();
    }

    private StatementModel createSecondStatement() {
        BlockModel blockModel = BlockModel.blockBuilder()
                .build();
        blockModel.setStatement("catch (NullPointerException e) {");
        blockModel.setStartIndex(75);
        blockModel.setEndIndex(106);
        blockModel.getStatements()
                .add(createSecondBlockStatement());
        blockModel.setEndOfBlockStatement(createSecondBlockEndStatement());

        return blockModel;
    }

    private StatementModel createSecondBlockStatement() {
        return StatementModel.statementBuilder()
                .statement("return null;")
                .startIndex(120)
                .endIndex(131)
                .build();
    }

    private StatementModel createSecondBlockEndStatement() {
        return StatementModel.statementBuilder()
                .statement("}")
                .startIndex(141)
                .endIndex(141)
                .build();
    }

    private Map<String, Map<String, List<MethodModel>>> createExpectedResult() {
        Map<String, Map<String, List<MethodModel>>> result = new HashMap<>();
        result.put(paths.get(FIRST_INDEX), createMethodsDetectionReturn());

        return result;
    }

    private void verifyFilesDetection() {
        verify(filesDetection, times(INVOKED_ONCE))
                .detect(eq(paths), eq(mimeType));
        verifyNoMoreInteractions(filesDetection);
    }

    private void verifyMethodsDetection() {
        verify(methodsDetection, times(INVOKED_ONCE))
                .detect(eq(createFileModels()));
        verifyNoMoreInteractions(methodsDetection);
    }

    private void verifyCodeSmellsDetection() {
        verify(codeSmellsDetection, times(INVOKED_ONCE))
                .detect(eq(createMethodModels()));
        verifyNoMoreInteractions(codeSmellsDetection);
    }
}