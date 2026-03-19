package sjj.oj.codesandbox.service.cpp;

import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import lombok.extern.slf4j.Slf4j;
import sjj.oj.codesandbox.model.ExecuteCodeRequest;
import sjj.oj.codesandbox.model.ExecuteCodeResponse;
import sjj.oj.codesandbox.model.ExecuteMessage;
import sjj.oj.codesandbox.model.JudgeInfo;
import sjj.oj.codesandbox.model.enums.JudgeInfoMessageEnum;
import sjj.oj.codesandbox.model.enums.QuestionSubmitStatusEnum;
import sjj.oj.codesandbox.service.CodeSandbox;
import sjj.oj.codesandbox.service.CommonCodeSandboxTemplate;
import sjj.oj.codesandbox.utils.ProcessUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * C++ code sandbox template.
 */
@Slf4j
public abstract class CppCodeSandboxTemplate extends CommonCodeSandboxTemplate implements CodeSandbox
{
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_CPP_FILE_NAME = "Main.cpp";
    private static final long TIME_OUT = 15000L;

    private static final List<String> blackList = Arrays.asList(
            "system(",
            "popen(",
            "fork(",
            "exec(",
            "socket(",
            "connect(",
            "CreateProcess(",
            "WinExec("
    );

    private static final WordTree WORD_TREE;

    static
    {
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(blackList);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest)
    {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();

        FoundWord foundWord = WORD_TREE.matchWord(code);
        if (foundWord != null)
        {
            return new ExecuteCodeResponse(
                    null,
                    "包含禁止词：" + foundWord.getFoundWord(),
                    QuestionSubmitStatusEnum.FAILED.getValue(),
                    new JudgeInfo(JudgeInfoMessageEnum.DANGEROUS_OPERATION.getValue(), null, null)
            );
        }

        File userCodeFile = saveCodeToFile(code, GLOBAL_CODE_DIR_NAME, GLOBAL_CPP_FILE_NAME);
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        if (compileFileExecuteMessage.getErrorMessage() != null)
        {
            return new ExecuteCodeResponse(
                    null,
                    compileFileExecuteMessage.getMessage(),
                    QuestionSubmitStatusEnum.FAILED.getValue(),
                    new JudgeInfo(compileFileExecuteMessage.getErrorMessage(), null, null)
            );
        }

        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);
        boolean deleted = deleteFile(userCodeFile);
        if (!deleted)
        {
            log.error("deleteFile error, userCodeFilePath = {}", userCodeFile.getAbsolutePath());
        }
        return outputResponse;
    }

    public ExecuteMessage compileFile(File userCodeFile)
    {
        String executablePath = userCodeFile.getParentFile().getAbsolutePath() + File.separator + "Main";
        try
        {
            Process compileProcess = new ProcessBuilder(
                    "g++",
                    "-O2",
                    "-std=c++17",
                    userCodeFile.getAbsolutePath(),
                    "-o",
                    executablePath
            ).start();
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            if (executeMessage.getExitValue() != 0)
            {
                executeMessage.setExitValue(1);
                executeMessage.setMessage(JudgeInfoMessageEnum.COMPILE_ERROR.getText());
                executeMessage.setErrorMessage(JudgeInfoMessageEnum.COMPILE_ERROR.getValue());
            }
            return executeMessage;
        }
        catch (Exception e)
        {
            ExecuteMessage executeMessage = new ExecuteMessage();
            executeMessage.setExitValue(1);
            executeMessage.setMessage(e.getMessage());
            executeMessage.setErrorMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getValue());
            return executeMessage;
        }
    }

    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList)
    {
        String executablePath = resolveExecutablePath(userCodeFile);
        if (inputList == null || inputList.isEmpty())
        {
            inputList = Arrays.asList("");
        }
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList)
        {
            try
            {
                Process runProcess = new ProcessBuilder(executablePath).start();
                writeInputToProcess(runProcess, inputArgs);
                AtomicBoolean timeOut = new AtomicBoolean(false);
                Thread timeoutWatcher = new Thread(() ->
                {
                    try
                    {
                        Thread.sleep(TIME_OUT);
                        if (runProcess.isAlive())
                        {
                            timeOut.set(true);
                            runProcess.destroy();
                        }
                    }
                    catch (InterruptedException ignored)
                    {
                        Thread.currentThread().interrupt();
                    }
                });
                timeoutWatcher.setDaemon(true);
                timeoutWatcher.start();

                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                timeoutWatcher.interrupt();
                if (timeOut.get())
                {
                    executeMessage.setExitValue(1);
                    executeMessage.setMessage(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getText());
                    executeMessage.setErrorMessage(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getValue());
                    executeMessage.setTime(TIME_OUT);
                }
                else if (executeMessage.getExitValue() != 0)
                {
                    executeMessage.setExitValue(1);
                    executeMessage.setMessage(JudgeInfoMessageEnum.RUNTIME_ERROR.getText());
                    executeMessage.setErrorMessage(JudgeInfoMessageEnum.RUNTIME_ERROR.getValue());
                }
                executeMessageList.add(executeMessage);
            }
            catch (Exception e)
            {
                ExecuteMessage executeMessage = new ExecuteMessage();
                executeMessage.setExitValue(1);
                executeMessage.setMessage(e.getMessage());
                executeMessage.setErrorMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getValue());
                executeMessageList.add(executeMessage);
            }
        }
        return executeMessageList;
    }

    private String resolveExecutablePath(File userCodeFile)
    {
        String parentPath = userCodeFile.getParentFile().getAbsolutePath();
        String executablePath = parentPath + File.separator + "Main";
        File executableFile = new File(executablePath);
        if (executableFile.exists())
        {
            return executablePath;
        }
        String windowsExecutablePath = executablePath + ".exe";
        File windowsExecutableFile = new File(windowsExecutablePath);
        if (windowsExecutableFile.exists())
        {
            return windowsExecutablePath;
        }
        return executablePath;
    }

    private void writeInputToProcess(Process runProcess, String inputArgs) throws IOException
    {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(runProcess.getOutputStream(), StandardCharsets.UTF_8)))
        {
            if (inputArgs != null)
            {
                writer.write(inputArgs);
            }
            writer.newLine();
            writer.flush();
        }
    }
}
