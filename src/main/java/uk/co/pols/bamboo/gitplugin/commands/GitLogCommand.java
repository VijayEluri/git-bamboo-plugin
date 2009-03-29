package uk.co.pols.bamboo.gitplugin.commands;

import com.atlassian.bamboo.commit.Commit;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GitLogCommand {
    private String gitExe;
    private File sourceCodeDirectory;
    private String lastRevisionChecked;

    public GitLogCommand(String gitExe, File sourceCodeDirectory, String lastRevisionChecked) {
        this.gitExe = gitExe;
        this.sourceCodeDirectory = sourceCodeDirectory;
        this.lastRevisionChecked = lastRevisionChecked;
    }

    public List<Commit> extractCommits() throws IOException {
        StringOutputStream stringOutputStream = new StringOutputStream();

        Execute execute = new Execute(new PumpStreamHandler(stringOutputStream));
        execute.setWorkingDirectory(sourceCodeDirectory);
        execute.setCommandline(getCommandLine());
        execute.execute();

        GitLogParser logParser = new GitLogParser(stringOutputStream.toString());

        stringOutputStream.close();
        List<Commit> commits = logParser.extractCommits();

        lastRevisionChecked = logParser.getMostRecentCommitDate();

        return commits;
    }

    public String getLastRevisionChecked() {
        return lastRevisionChecked;
    }

    private String[] getCommandLine() {
        if (lastRevisionChecked != null) {
            return new String[]{gitExe, "log", "--date=iso8601", "--since=\"" + lastRevisionChecked + "\""};
        }
        return new String[]{gitExe, "log", "-1", "--date=iso8601"};
    }
}