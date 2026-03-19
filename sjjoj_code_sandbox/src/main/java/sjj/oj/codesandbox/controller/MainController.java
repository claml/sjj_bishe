package sjj.oj.codesandbox.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sjj.oj.codesandbox.model.ExecuteCodeRequest;
import sjj.oj.codesandbox.model.ExecuteCodeResponse;
import sjj.oj.codesandbox.model.enums.QuestionSubmitStatusEnum;
import sjj.oj.codesandbox.model.enums.SupportLanguageEnum;
import sjj.oj.codesandbox.service.c.CNativeCodeSandbox;
import sjj.oj.codesandbox.service.cpp.CppNativeCodeSandbox;
import sjj.oj.codesandbox.service.java.JavaNativeCodeSandbox;
import sjj.oj.codesandbox.service.python3.Python3Native3CodeSandbox;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Code sandbox HTTP entry.
 */
@RestController
@RequestMapping("/codesandbox")
public class MainController
{
    private static final String AUTH_REQUEST_HEADER = "oj-codesandbox-auth-by-sjj";
    private static final String AUTH_REQUEST_SECRET = "$W$~vrZwe7z&L!ht^U%fF2zZzHTjWSwY%@ZeEJ^*(qZ()D3npx";

    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    @Resource
    private Python3Native3CodeSandbox python3NativeCodeSandbox;

    @Resource
    private CNativeCodeSandbox cNativeCodeSandbox;

    @Resource
    private CppNativeCodeSandbox cppNativeCodeSandbox;

    @GetMapping("/healthCheck")
    public String healthCheck()
    {
        return "I'm fine";
    }

    @GetMapping("/intro")
    public String selfIntroduction()
    {
        return "Supported languages: " + SupportLanguageEnum.getValues();
    }

    @PostMapping("/run")
    public ExecuteCodeResponse runCode(@RequestBody ExecuteCodeRequest executeCodeRequest,
                                       HttpServletRequest request,
                                       HttpServletResponse response)
    {
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader))
        {
            response.setStatus(403);
            return new ExecuteCodeResponse(null, "Authentication failed", QuestionSubmitStatusEnum.FAILED.getValue(), null);
        }
        if (executeCodeRequest == null)
        {
            throw new RuntimeException("Request body is empty");
        }

        String language = executeCodeRequest.getLanguage();
        if (SupportLanguageEnum.JAVA.getValue().equals(language))
        {
            return javaNativeCodeSandbox.executeCode(executeCodeRequest);
        }
        if (SupportLanguageEnum.PYTHON3.getValue().equals(language))
        {
            return python3NativeCodeSandbox.executeCode(executeCodeRequest);
        }
        if (SupportLanguageEnum.C.getValue().equals(language))
        {
            return cNativeCodeSandbox.executeCode(executeCodeRequest);
        }
        if (SupportLanguageEnum.CPP.getValue().equals(language))
        {
            return cppNativeCodeSandbox.executeCode(executeCodeRequest);
        }
        return new ExecuteCodeResponse(
                null,
                "Unsupported language: " + language + "; supported: " + SupportLanguageEnum.getValues(),
                QuestionSubmitStatusEnum.FAILED.getValue(),
                null
        );
    }
}
