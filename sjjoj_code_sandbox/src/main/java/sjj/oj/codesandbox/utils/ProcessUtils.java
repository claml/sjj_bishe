package sjj.oj.codesandbox.utils;

import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;
import sjj.oj.codesandbox.model.ExecuteMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Process utility.
 */
public class ProcessUtils {

    /**
     * Run process and collect output.
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        long initialMemory = getUsedMemory();
        StopWatch stopWatch = new StopWatch();
        try {
            stopWatch.start();
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            executeMessage.setMessage(readStreamSafely(runProcess.getInputStream()));
            executeMessage.setErrorMessage(readStreamSafely(runProcess.getErrorStream()));
            if (exitValue == 0) {
                System.out.println(opName + " success");
                executeMessage.setErrorMessage(null);
            } else {
                System.out.println(opName + " failed, exit code: " + exitValue);
            }
            long finalMemory = getUsedMemory();
            long memoryUsage = finalMemory - initialMemory;
            executeMessage.setMemory(memoryUsage / 1024);
        } catch (Exception e) {
            if (executeMessage.getExitValue() == null) {
                executeMessage.setExitValue(1);
            }
            if (StringUtils.isBlank(executeMessage.getMessage())) {
                executeMessage.setMessage("");
            }
            executeMessage.setErrorMessage(StringUtils.defaultIfBlank(e.getMessage(), "Process execute error"));
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            executeMessage.setTime(stopWatch.getTotalTimeMillis());
        }
        return executeMessage;
    }

    private static String readStreamSafely(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }
        try (BufferedReader bufferedReader =
                     new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<String> outputStrList = new ArrayList<>();
            String outputLine;
            while ((outputLine = bufferedReader.readLine()) != null) {
                outputStrList.add(outputLine);
            }
            return StringUtils.join(outputStrList, "\n");
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Run interactive process and collect output.
     */
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s) + "\n";
            outputStreamWriter.write(join);
            outputStreamWriter.flush();

            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }

    /**
     * Get used heap memory in bytes.
     */
    public static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
