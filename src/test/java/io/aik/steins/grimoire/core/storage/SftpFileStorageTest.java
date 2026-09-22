package io.aik.steins.grimoire.core.storage;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import io.aik.steins.grimoire.core.config.FileStorageConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SftpFileStorage 存储策略单元测试 -anchor
 * <p>覆盖 upload / download / remove / exists 四条主路径，以及远端路径拼接（basePath 补斜杠、
 * 空白 basePath 回退根目录）、会话装配（私钥 / 密码 / StrictHostKeyChecking）与异常传播。</p>
 * <p>外部依赖全部 mock：{@link SftpFileStorage} 内部的 {@code new JSch()} 由 {@code mockConstruction}
 * 拦截，{@link Session} 与 {@link ChannelSftp} 均为 Mockito mock。真实网络连接只可能发生在 JSch 实例上，
 * 而本测试中该实例是 mock，{@code session.connect()} / {@code channel.put()} 均为空操作，
 * 因此测试期间不会连接 application.yml 中的 {@code sftp.host=192.168.229.128} 或任何端点。</p>
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote 依赖 src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker（inline mock maker）
 * @since 2026/09/20
 * -
 **/
@DisplayName("SFTP 存储策略测试")
class SftpFileStorageTest {

    private static final String HOST = "192.168.229.128";
    private static final int PORT = 22;
    private static final String USERNAME = "grimoire";
    private static final String PASSWORD = "sftp-password";
    private static final String PRIVATE_KEY = "/home/grimoire/.ssh/id_rsa";
    private static final String BASE_PATH = "/srv/grimoire/files";
    private static final String REMOTE_PATH = "/srv/grimoire/files/2026/09/aik.pdf";
    private static final byte[] PAYLOAD = "grimoire-sftp-payload".getBytes(StandardCharsets.UTF_8);

    private Session session;
    private ChannelSftp channel;
    private MockedConstruction<JSch> jschConstruction;

    @BeforeEach
    void setUp() throws Exception {
        session = mock(Session.class);
        channel = mock(ChannelSftp.class);
        when(session.isConnected()).thenReturn(true);
        when(channel.isConnected()).thenReturn(true);
        when(session.openChannel("sftp")).thenReturn(channel);
        jschConstruction = Mockito.mockConstruction(JSch.class,
                (jsch, context) -> when(jsch.getSession(USERNAME, HOST, PORT)).thenReturn(session));
    }

    @AfterEach
    void tearDown() {
        jschConstruction.close();
    }

    // ==================== 测试夹具 ====================

    private static FileStorageConfig sftpConfig(String basePath, String password, String privateKey) {
        FileStorageConfig.SftpConfig sftp = new FileStorageConfig.SftpConfig();
        sftp.setHost(HOST);
        sftp.setPort(PORT);
        sftp.setUsername(USERNAME);
        sftp.setPassword(password);
        sftp.setPrivateKey(privateKey);
        sftp.setBasePath(basePath);
        FileStorageConfig.MethodConfig method = new FileStorageConfig.MethodConfig();
        method.setSftp(sftp);
        FileStorageConfig config = new FileStorageConfig();
        config.setUse("sftp");
        config.setMethod(method);
        return config;
    }

    private static SftpFileStorage storage(String basePath) {
        return new SftpFileStorage(sftpConfig(basePath, PASSWORD, PRIVATE_KEY));
    }

    private static SftpFileStorage storageWithoutCredentials() {
        return new SftpFileStorage(sftpConfig(BASE_PATH, null, null));
    }

    /**
     * 取回被拦截的真实构造点对应的 JSch mock，并断言只有一次构造
     */
    private JSch constructedJsch() {
        assertThat(jschConstruction.constructed()).hasSize(1);
        return jschConstruction.constructed().get(0);
    }

    // ==================== upload ====================

    @Test
    @DisplayName("upload：远端路径 = basePath + 雪花名，扩展名保留且单斜杠")
    void upload_shouldAppendStoredNameToNormalizedBasePath() throws Exception {
        SftpFileStorage storage = storage(BASE_PATH);
        InputStream inputStream = new ByteArrayInputStream(PAYLOAD);

        String remotePath = storage.upload(inputStream, "魔典封面.png");

        assertThat(remotePath).matches("/srv/grimoire/files/\\d+\\.png");
        assertThat(remotePath).doesNotContain("//");
        verify(channel).put(inputStream, remotePath);
        verify(session).connect();
        verify(channel).connect();
        verify(channel).disconnect();
        verify(session).disconnect();
    }

    @Test
    @DisplayName("upload：basePath 已带 / 时不产生双斜杠")
    void upload_basePathWithTrailingSlash_shouldNotDoubleSlash() throws Exception {
        String remotePath = storage(BASE_PATH + "/").upload(new ByteArrayInputStream(PAYLOAD), "a.txt");

        assertThat(remotePath).matches("/srv/grimoire/files/\\d+\\.txt");
        assertThat(remotePath).doesNotContain("//");
    }

    @Test
    @DisplayName("upload：basePath 为空白时回退到根目录")
    void upload_blankBasePath_shouldFallbackToRoot() throws Exception {
        String remotePath = storage("   ").upload(new ByteArrayInputStream(PAYLOAD), "a.txt");

        assertThat(remotePath).matches("/\\d+\\.txt");
    }

    @Test
    @DisplayName("upload：扩展名统一小写")
    void upload_shouldLowercaseExtension() throws Exception {
        String remotePath = storage(BASE_PATH).upload(new ByteArrayInputStream(PAYLOAD), "Report.PDF");

        assertThat(remotePath).matches("/srv/grimoire/files/\\d+\\.pdf");
    }

    @Test
    @DisplayName("createSession：装配私钥、密码与 StrictHostKeyChecking=no")
    void upload_shouldConfigureSessionWithPrivateKeyPasswordAndHostKeyChecking() throws Exception {
        storage(BASE_PATH).upload(new ByteArrayInputStream(PAYLOAD), "a.txt");

        JSch jsch = constructedJsch();
        verify(jsch).getSession(USERNAME, HOST, PORT);
        verify(jsch).addIdentity(PRIVATE_KEY);
        verify(session).setPassword(PASSWORD);
        ArgumentCaptor<Properties> captor = ArgumentCaptor.forClass(Properties.class);
        verify(session).setConfig(captor.capture());
        assertThat(captor.getValue()).containsEntry("StrictHostKeyChecking", "no");
        verify(session).connect();
    }

    @Test
    @DisplayName("createSession：无凭证时不设置私钥与密码")
    void upload_withoutCredentials_shouldSkipIdentityAndPassword() throws Exception {
        storageWithoutCredentials().upload(new ByteArrayInputStream(PAYLOAD), "a.txt");

        JSch jsch = constructedJsch();
        verify(jsch, never()).addIdentity(anyString());
        verify(session, never()).setPassword(anyString());
        verify(session).connect();
    }

    // ==================== download ====================

    @Test
    @DisplayName("download：把远端字节写入返回结果并断开连接")
    void download_shouldStreamRemoteBytesIntoResult() throws Exception {
        doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(1);
            outputStream.write(PAYLOAD);
            return null;
        }).when(channel).get(eq(REMOTE_PATH), any(OutputStream.class));

        byte[] data = storage(BASE_PATH).download(REMOTE_PATH);

        assertThat(data).isEqualTo(PAYLOAD);
        verify(channel).get(eq(REMOTE_PATH), any(OutputStream.class));
        verify(channel).disconnect();
        verify(session).disconnect();
    }

    // ==================== remove ====================

    @Test
    @DisplayName("remove：调用 rm 并返回 true")
    void remove_shouldCallRmAndReturnTrue() throws Exception {
        boolean removed = storage(BASE_PATH).remove(REMOTE_PATH);

        assertThat(removed).isTrue();
        verify(channel).rm(REMOTE_PATH);
        verify(channel).disconnect();
        verify(session).disconnect();
    }

    // ==================== exists ====================

    @Test
    @DisplayName("exists：ls 成功时返回 true")
    void exists_shouldReturnTrueWhenLsSucceeds() throws Exception {
        when(channel.ls(REMOTE_PATH)).thenReturn(new Vector<ChannelSftp.LsEntry>());

        assertThat(storage(BASE_PATH).exists(REMOTE_PATH)).isTrue();
        verify(channel).ls(REMOTE_PATH);
    }

    @Test
    @DisplayName("exists：ls 抛异常时返回 false 且仍断开连接")
    void exists_shouldReturnFalseWhenLsThrows() throws Exception {
        when(channel.ls(REMOTE_PATH)).thenThrow(new SftpException(2, "No such file"));

        assertThat(storage(BASE_PATH).exists(REMOTE_PATH)).isFalse();
        verify(channel).disconnect();
        verify(session).disconnect();
    }

    // ==================== 异常传播 ====================

    @Test
    @DisplayName("upload：put 失败时异常向上传播且仍断开连接")
    void upload_whenPutFails_shouldPropagateAndStillDisconnect() throws Exception {
        doThrow(new SftpException(4, "put failed")).when(channel).put(any(InputStream.class), anyString());

        SftpFileStorage storage = storage(BASE_PATH);
        InputStream inputStream = new ByteArrayInputStream(PAYLOAD);

        assertThatThrownBy(() -> storage.upload(inputStream, "a.txt")).isInstanceOf(SftpException.class);
        verify(channel).disconnect();
        verify(session).disconnect();
    }

    @Test
    @DisplayName("download：session 连接失败时异常向上传播且不触碰 sftp 通道")
    void download_whenConnectFails_shouldPropagateWithoutChannel() throws Exception {
        doThrow(new JSchException("connect refused")).when(session).connect();

        SftpFileStorage storage = storage(BASE_PATH);

        assertThatThrownBy(() -> storage.download(REMOTE_PATH)).isInstanceOf(JSchException.class);
        verify(session).connect();
        verify(channel, never()).connect();
        verify(channel, never()).disconnect();
    }

    // ==================== 零真实网络连接证据 ====================

    @Test
    @DisplayName("外部依赖全部为 mock：JSch / Session / ChannelSftp 无真实实现被触达")
    void externalDependencies_shouldBeFullyMocked() throws Exception {
        storage(BASE_PATH).upload(new ByteArrayInputStream(PAYLOAD), "a.txt");

        JSch jsch = constructedJsch();
        assertThat(mockingDetails(jsch).isMock()).isTrue();
        assertThat(mockingDetails(session).isMock()).isTrue();
        assertThat(mockingDetails(channel).isMock()).isTrue();
        // 唯一的对外入口（JSch 实例）为 mock，connect() 落在 Session mock 上
        verify(jsch).getSession(USERNAME, HOST, PORT);
        verify(session).connect();
        verify(channel).put(any(InputStream.class), anyString());
    }
}
