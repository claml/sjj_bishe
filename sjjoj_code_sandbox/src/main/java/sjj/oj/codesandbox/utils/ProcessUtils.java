package sjj.oj.codesandbox.utils;

import cn.hutool.core.util.StrUtil;
import sjj.oj.codesandbox.model.ExecuteMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 进程工具类
 *
 * @author sjj
 */
public class ProcessUtils
{

    /**
     * 执行进程并获取信息
     *
     * @param runProcess
     * @param opName
     * @return
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
     * 执行交互式进程并获取信息
     *
     * @param runProcess
     * @param args
     * @return
     */
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String args)
    {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try
        {
            // 向控制台输入程序
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s) + "\n";
            outputStreamWriter.write(join);
            // 相当于按了回车，执行输入的发送
            outputStreamWriter.flush();

            // 分批获取进程的正常输出
            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            // 逐行读取
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null)
            {
                compileOutputStringBuilder.append(compileOutputLine);
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());
            // 记得资源的释放，否则会卡死
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return executeMessage;
    }

    /**
     * 获取当前已使用的内存量
     * 单位是byte
     *
     * @return
     */
    public static long getUsedMemory()
    {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
