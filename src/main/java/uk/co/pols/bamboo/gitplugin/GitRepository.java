package uk.co.pols.bamboo.gitplugin;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.commit.Commit;
import com.atlassian.bamboo.repository.AbstractRepository;
import com.atlassian.bamboo.repository.InitialBuildAwareRepository;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.BuildChanges;
import com.atlassian.bamboo.v2.build.BuildChangesImpl;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides GIT and GITHUB support for the Bamboo Build Server
 * <p/>
 * TODO Let user define the location of the git exe
 * TODO run a which git command to guess the location of git
 * TODO Add hook for github callback triggering the build
 * TODO don't include all historical commits on first build
 * TODO work out if the repository url has changed...
 * <p/>
 * This is what capistarno does....
 * git reset -q --hard 10e162370493a984c279ffc7ca59e18d7850e844;
 * git checkout -q -b deploy 10e162370493a984c279ffc7ca59e18d7850e844;
 * <p/>
 * So if I can do a remote history I'm laughing...
 */
public class GitRepository extends AbstractRepository implements /*SelectableAuthenticationRepository, WebRepositoryEnabledRepository,*/ InitialBuildAwareRepository /*, RepositoryEventAware*/ {
    private static final String GIT_HOME = "/opt/local/bin";
    private static final String GIT_EXE = GIT_HOME + "/git";

    public static final String NAME = "Git";
    public static final String KEY = "git";
    public static final String REPO_PREFIX = "repository.git.";

    private GitRepositoryConfig gitRepositoryConfig = gitRepositoryConfig();

    public synchronized BuildChanges collectChangesSinceLastBuild(String planKey, String lastVcsRevisionKey) throws RepositoryException {
        List<Commit> commits = new ArrayList<Commit>();

        String latestCommitTime = gitClient().getLatestUpdate(
                buildLoggerManager.getBuildLogger(planKey),
                gitRepositoryConfig.getRepositoryUrl(),
                planKey,
                lastVcsRevisionKey,
                commits,
                getSourceCodeDirectory(planKey)
        );

        return new BuildChangesImpl(String.valueOf(latestCommitTime), commits);
    }

    public String retrieveSourceCode(String planKey, String vcsRevisionKey) throws RepositoryException {
        return gitClient().initialiseRepository(
                getSourceCodeDirectory(planKey),
                planKey,
                vcsRevisionKey,
                gitRepositoryConfig,
                isWorkspaceEmpty(getSourceCodeDirectory(planKey)),
                buildLoggerManager.getBuildLogger(planKey));
    }

    @Override
    public ErrorCollection validate(BuildConfiguration buildConfiguration) {
        return gitRepositoryConfig.validate(super.validate(buildConfiguration), buildConfiguration);
    }

    public boolean isRepositoryDifferent(Repository repository) {
        if (repository instanceof GitRepository) {
            GitRepository gitRepository = (GitRepository) repository;
            return !new EqualsBuilder()
                    .append(this.getName(), gitRepository.getName())
                    .append(getRepositoryUrl(), gitRepository.getRepositoryUrl())
                    .isEquals();
        }
        return true;
    }

    public void prepareConfigObject(BuildConfiguration buildConfiguration) {
    }

    @Override
    public void populateFromConfig(HierarchicalConfiguration config) {
        super.populateFromConfig(config);
        gitRepositoryConfig.populateFromConfig(config);
    }

    @Override
    public HierarchicalConfiguration toConfiguration() {
        return gitRepositoryConfig.toConfiguration(super.toConfiguration());
    }

    public void onInitialBuild(BuildContext buildContext) {
        // do nothing
    }

    public String getName() {
        return NAME;
    }

    public String getUrl() {
        return "http://github.com/guides/home";
    }

    public void setRepositoryUrl(String repositoryUrl) {
        gitRepositoryConfig.setRepositoryUrl(repositoryUrl);
    }

    public String getRepositoryUrl() {
        return gitRepositoryConfig.getRepositoryUrl();
    }

    public String getBranch() {
        return gitRepositoryConfig.getBranch();
    }

    public void setBranch(String branch) {
        gitRepositoryConfig.setBranch(branch);
    }

    @Override
    public String getWebRepositoryUrlForCommit(Commit commit) {
        return "noidea";
    }

    public String getHost() {
        return gitRepositoryConfig.getHost();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(101, 11)
                .append(getKey())
                .append(getRepositoryUrl())
                .append(getTriggerIpAddress())
                .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GitRepository)) {
            return false;
        }
        GitRepository rhs = (GitRepository) o;
        return new EqualsBuilder()
                .append(getRepositoryUrl(), rhs.getRepositoryUrl())
                .append(getTriggerIpAddress(), rhs.getTriggerIpAddress())
                .isEquals();
    }

    public int compareTo(Object obj) {
        GitRepository o = (GitRepository) obj;
        return new CompareToBuilder()
                .append(getRepositoryUrl(), o.getRepositoryUrl())
                .append(getTriggerIpAddress(), o.getTriggerIpAddress())
                .toComparison();
    }

//    public List<NameValuePair> getAuthenticationTypes() {
//        List<NameValuePair> types = new ArrayList<NameValuePair>();
//        types.add(AuthenticationType.PASSWORD.getNameValue());
//        types.add(AuthenticationType.SSH.getNameValue());
//        return types;
//    }

    protected GitClient gitClient() {
        return new CmdLineGitClient(GIT_EXE);
    }

    protected GitRepositoryConfig gitRepositoryConfig() {
        return new GitRepositoryConfig();
    }
}