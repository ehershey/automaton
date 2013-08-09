package net.jimj.automaton.commands;

public class ArgUtil {
    public static String[] split(String args) {
        if(args == null) {
            args = "";
        }

        return args.split(" ");
    }

    public static String squash(String[] args, int fromPos) {
        StringBuilder result = new StringBuilder();
        for(int i=fromPos;i<args.length;i++) {
            result.append(args[i]).append(" ");
        }
        return result.toString().trim();
    }
}
