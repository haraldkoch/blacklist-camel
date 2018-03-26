package net.cfrq.blacklist;

import java.util.List;

public class FormatAsShellScript {
    public static String process(List<String> commands) {
        StringBuilder script = new StringBuilder();

        script.append("#!/bin/sh\n\n");

        for (String command : commands) {
            script.append(command);
            script.append("\n");
        }
        
        return script.toString();
    }
}
