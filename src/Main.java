package src;

import static src.Utils.error;


/** Driver class for the system
 *  @author Kaung Si Thu
 */
public class Main {

    /** Usage: java src.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                throw error("Please enter a command.");
            }

            String firstArg = args[0];
            switch (firstArg) {
                case "init" -> {
                    checkSize(args.length, 1);
                    CommandAssembler.init();
                }
                case "add" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 2);
                    CommandAssembler.add(args[1]);
                }
                case "checkout" -> {
                    Repository.checkGitletRepo();
                    // checkSize(args.length, 2, 3, 4);
                    if (args.length == 3 && args[1].equals("--")) {
                        CommandAssembler.checkout(null, null, args[2]);
                    } else if (args.length == 2) {
                        CommandAssembler.checkout(args[1], null, null);
                    } else if (args.length == 4 && args[2].equals("--")) {
                        CommandAssembler.checkout(null, args[1], args[3]);
                    } else {
                        throw error("Incorrect operands.");
                    }
                }
                case "log" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 1);
                    CommandAssembler.log();
                }
                case "commit" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 2);
                    CommandAssembler.commit(args[1], false, null);
                }
                case "rm" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 2);
                    CommandAssembler.rm(args[1]);
                }
                case "global-log" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 1);
                    CommandAssembler.globallog();
                }
                case "find" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 2);
                    CommandAssembler.find(args[1]);
                }
                case "status" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 1);
                    CommandAssembler.status();
                }
                case "branch" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 2);
                    CommandAssembler.branch(args[1]);
                }
                case "rm-branch" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 2);
                    CommandAssembler.removeBranch(args[1]);
                }
                case "reset" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 2);
                    CommandAssembler.reset(args[1]);
                }
                case "merge" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 2);
                    CommandAssembler.merge(args[1]);
                }
                case "add-remote" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 3);
                    CommandAssembler.addRemote(args[1], args[2]);
                }
                case "rm-remote" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 2);
                    CommandAssembler.removeRemote(args[1]);
                }
                case "push" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 3);
                    CommandAssembler.remotePush(args[1], args[2]);
                }
                case "fetch" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 3);
                    CommandAssembler.remoteFetch(args[1], args[2]);
                }
                case "pull" -> {
                    Repository.checkGitletRepo();
                    checkSize(args.length, 3);
                    CommandAssembler.remotePull(args[1], args[2]);
                }
                default -> throw error("No command with that name exists.");
            }
        } catch (Exception e) {
            System.out.print(e.getMessage() + '\n');
            System.exit(0);
        }
    }

    private static void checkSize(int argSize, int... vals) {
        for (int size : vals) {
            if (argSize == size) {
                return;
            }
        }
        throw error("Incorrect operands.");
    }
}
