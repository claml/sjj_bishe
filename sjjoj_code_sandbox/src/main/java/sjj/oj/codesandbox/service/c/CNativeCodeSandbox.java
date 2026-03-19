package sjj.oj.codesandbox.service.c;

import org.springframework.stereotype.Component;
import sjj.oj.codesandbox.model.ExecuteCodeRequest;
import sjj.oj.codesandbox.model.ExecuteCodeResponse;

/**
 * Native C code sandbox implementation.
 */
@Component
public class CNativeCodeSandbox extends CCodeSandboxTemplate
{
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest)
    {
        return super.executeCode(executeCodeRequest);
    }
}
