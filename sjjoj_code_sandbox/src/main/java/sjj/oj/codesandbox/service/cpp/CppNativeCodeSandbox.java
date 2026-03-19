package sjj.oj.codesandbox.service.cpp;

import org.springframework.stereotype.Component;
import sjj.oj.codesandbox.model.ExecuteCodeRequest;
import sjj.oj.codesandbox.model.ExecuteCodeResponse;

/**
 * Native C++ code sandbox implementation.
 */
@Component
public class CppNativeCodeSandbox extends CppCodeSandboxTemplate
{
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest)
    {
        return super.executeCode(executeCodeRequest);
    }
}
