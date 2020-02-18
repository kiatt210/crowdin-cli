package com.crowdin.cli.commands;

import com.crowdin.cli.commands.functionality.DryrunProjectFiles;
import com.crowdin.cli.commands.functionality.ProjectProxy;
import com.crowdin.cli.commands.parts.PropertiesBuilderCommandPart;
import com.crowdin.cli.properties.PropertiesBean;
import com.crowdin.cli.utils.console.ConsoleSpinner;
import com.crowdin.common.Settings;
import com.crowdin.common.models.Branch;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

import static com.crowdin.cli.utils.MessageSource.Messages.FETCHING_PROJECT_INFO;
import static com.crowdin.cli.utils.console.ExecutionStatus.ERROR;
import static com.crowdin.cli.utils.console.ExecutionStatus.OK;

@CommandLine.Command(
    name = "project",
    customSynopsis = "@|fg(yellow) crowdin list project|@ [CONFIG OPTIONS] [OPTIONS]",
    description = "Show a list of source files in the current project")
public class ListProjectSubcommand extends PropertiesBuilderCommandPart {

    @CommandLine.Option(names = {"-b", "--branch"}, paramLabel = "...", description = "Specify branch name. Default: none")
    protected String branch;

    @CommandLine.Option(names = {"--tree"}, description = "List contents of directories in a tree-like format")
    protected boolean treeView;

    @Override
    public void run() {
        PropertiesBean pb = this.buildPropertiesBean();
        Settings settings = Settings.withBaseUrl(pb.getApiToken(), pb.getBaseUrl());

        ProjectProxy project = new ProjectProxy(pb.getProjectId(), settings);
        try {
            ConsoleSpinner.start(FETCHING_PROJECT_INFO.getString(), this.noProgress);
            project.downloadProject()
                .downloadDirectories()
                .downloadFiles()
                .downloadBranches();
            ConsoleSpinner.stop(OK);
        } catch (Exception e) {
            ConsoleSpinner.stop(ERROR);
            throw new RuntimeException("Exception while gathering project info", e);
        }

        Long branchId = (StringUtils.isNotEmpty(this.branch))
            ? project.getBranchByName(this.branch)
                .map(Branch::getId)
                .orElseThrow(() -> new RuntimeException("Couldn't find branchId by that name"))
            : null;

        (new DryrunProjectFiles(project.getFiles(), project.getMapDirectories(), project.getMapBranches(), branchId)).run(treeView);
    }
}
