/*
 * The MIT License
 *
 * Copyright (C) 2010-2011 by Anthony Robinson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.publish_over;

import hudson.FilePath;
import hudson.model.TaskListener;
import jenkins.plugins.publish_over.helper.BPBuildInfoFactory;
import jenkins.plugins.publish_over.helper.RandomFile;
import org.apache.commons.lang.StringUtils;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TreeMap;

import static jenkins.plugins.publish_over.helper.InputStreamMatcher.streamContains;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({ "PMD.SignatureDeclareThrowsException", "PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals" })
public class BPTransferTest {

    @Rule // FindBugs: must be public for the @Rule to work
    public TemporaryFolder baseDir = new TemporaryFolder();
    private final TreeMap<String, String> envVars = new TreeMap<String, String>(); // NOPMD
    private final IMocksControl mockControl = EasyMock.createStrictControl();
    private final BPClient mockClient = mockControl.createMock(BPClient.class);
    private BPBuildInfo buildInfo;

    @Before
    public void setUp() throws Exception {
        final BPBuildEnv current = new BPBuildInfoFactory().createEmptyBuildEnv();
        buildInfo = new BPBuildInfo(TaskListener.NULL, "", new FilePath(new File("")), current, null);
        buildInfo.setEnvVars(envVars);
        buildInfo.setBaseDirectory(new FilePath(baseDir.getRoot()));
        buildInfo.setBuildTime(Calendar.getInstance());
    }

    @Test public void testSingleFileInRootWithExplicitPath() throws Exception {
        assertSingleFileInRoot("xxx.log", "xxx.log");
    }

    @Test public void testSingleFileInRootWithExtensionGlob() throws Exception {
        assertSingleFileInRoot("xxx.log", "xxx.*");
    }

    @Test public void testSingleFileNoExtensionInRootWithMatchAnyFile() throws Exception {
        assertSingleFileInRoot("xxx", "*");
    }

    @Test public void testSingleFileInRootWithMatchAnyFile() throws Exception {
        assertSingleFileInRoot("xxx.log", "*");
    }

    @Test public void testSingleFileInRootWithMatchAnyFileWithAnyExtension() throws Exception {
        assertSingleFileInRoot("xxx.log", "*.*");
    }

    @Test public void testSingleFileInRootWithMatchAnyFileWithAnyPathAndExtension() throws Exception {
        assertSingleFileInRoot("xxx.log", "**/*.*");
    }

    @Test public void testSingleFileInRootWithMatchAnyFileWithAnyPathAndFile() throws Exception {
        assertSingleFileInRoot("xxx.log", "**/*");
    }

    @Test public void testSingleFileInRootWithMatchAnyFileWithAnyPathAndFileWin() throws Exception {
        assertSingleFileInRoot("xxx.log", "**\\*");
    }

    @Test public void testSingleFileNoExtensionInRootWithMatchAnyFileWithAnyPathAndFile() throws Exception {
        assertSingleFileInRoot("xxx", "**/*");
    }

    private void assertSingleFileInRoot(final String filename, final String pattern) throws Exception {
        final RandomFile toTransfer = new RandomFile(baseDir.getRoot(), filename);
        final BPTransfer transfer = new BPTransfer(pattern, "", "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expectTransferFile(transfer, toTransfer);
        assertTransfer(transfer, 1);
    }

    @Test public void testExceptionPropagatesWhenFailToTransferFile() throws Exception {
        final RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "abc.jpg");
        final BPTransfer transfer = new BPTransfer("*", "", "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        final Exception someException = new Exception("meh");
        mockClient.transferFile(EasyMock.same(transfer),
                EasyMock.eq(new FilePath(toTransfer.getFile())), streamContains(toTransfer.getContents()));
        expectLastCall().andThrow(someException);
        try {
            replayAndTransfer(transfer);
            fail();
        } catch (BapTransferException bte) {
            assertSame(someException, bte.getCause());
        }
    }

    @Test public void testMultipleFiles() throws Exception {
        final RandomFile log1 = new RandomFile(baseDir.getRoot(), "one.log");
        final RandomFile log2 = new RandomFile(baseDir.getRoot(), "two.log");
        final RandomFile log3 = new RandomFile(baseDir.getRoot(), "three.log");
        new RandomFile(baseDir.getRoot(), "notALog.txt");
        final BPTransfer transfer = new BPTransfer("*.log", "", "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        mockControl.checkOrder(false);
        expectTransferFile(transfer, log1, log2, log3);
        mockControl.checkOrder(true);
        final int expectedFileCount = 3;
        assertTransfer(transfer, expectedFileCount);
    }

    @Test public void testEnvVarInPattern() throws Exception {
        final RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello_123.txt");
        final BPTransfer transfer = new BPTransfer("hello_${BUILD_NUMBER}.*", "", "", false, false);
        envVars.put("BUILD_NUMBER", "123");
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expectTransferFile(transfer, toTransfer);
        assertTransfer(transfer, 1);
    }

    @Test public void testCreateSingleDirectoryFromRemoteDirectory() throws Exception {
        final RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        final String dir = "newDir";
        final BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), dir, "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(dir)).andReturn(false);
        expect(mockClient.makeDirectory(dir)).andReturn(true);
        expect(mockClient.changeDirectory(dir)).andReturn(true);
        expectTransferFile(transfer, toTransfer);
        assertTransfer(transfer, 1);
    }

    @Test public void testCreateSingleDirectoryFromRemoteDirectoryWithEnvVar() throws Exception {
        final RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        envVars.put("BUILD_NUMBER", "123");
        final String expandedDir = "newDir-123";
        final BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), "newDir-${BUILD_NUMBER}", "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(expandedDir)).andReturn(false);
        expect(mockClient.makeDirectory(expandedDir)).andReturn(true);
        expect(mockClient.changeDirectory(expandedDir)).andReturn(true);
        expectTransferFile(transfer, toTransfer);
        assertTransfer(transfer, 1);
    }

    @Test public void testCreateMultipleDirectoriesFromRemoteDirectory() throws Exception {
        assertCreateMultipleDirectoriesFromRemoteDirectory("newDir/and/another", new String[] {"newDir", "and", "another"});
    }

    @Test public void testCreateMultipleDirectoriesFromRemoteDirectoryWin() throws Exception {
        assertCreateMultipleDirectoriesFromRemoteDirectory("newDir\\and\\another", new String[] {"newDir", "and", "another"});
    }

    @Test public void testCreateMultipleDirectoriesFromRemoteDirectoryTrailingSeparator() throws Exception {
        assertCreateMultipleDirectoriesFromRemoteDirectory("newDir/and/another/", new String[] {"newDir", "and", "another"});
    }

    @Test public void testCreateMultipleDirectoriesFromRemoteDirectoryTrailingSeparatorWin() throws Exception {
        assertCreateMultipleDirectoriesFromRemoteDirectory("newDir\\and\\another\\", new String[] {"newDir", "and", "another"});
    }

    private void assertCreateMultipleDirectoriesFromRemoteDirectory(final String remoteDir, final String[] expectedDirs) throws Exception {
        final String normalizedDir = remoteDir.contains("\\") ? remoteDir.replaceAll("\\\\", "/") : remoteDir;
        final RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        final BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), remoteDir, "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(normalizedDir)).andReturn(false);
        expect(mockClient.makeDirectory(normalizedDir)).andReturn(false);
        for (String subDir : expectedDirs) {
            expect(mockClient.changeDirectory(subDir)).andReturn(false);
            expect(mockClient.makeDirectory(subDir)).andReturn(true);
            expect(mockClient.changeDirectory(subDir)).andReturn(true);
        }
        expectTransferFile(transfer, toTransfer);
        assertTransfer(transfer, 1);
    }


    @Test public void testCreateDirectoriesFromSrcFileAndRemoteDirectory() throws Exception {
        final RandomFile srcFile = new RandomFile(baseDir.getRoot(), "bit/of/a/trek/to/my.file");
        assertCreateDirectories(srcFile, "my/remote/dir", new String[] {"my", "remote", "dir"},
            new String[] {"bit", "of", "a", "trek", "to"});
    }

    private void assertCreateDirectories(final RandomFile srcFile, final String remoteDir, final String[] expectedDirsRemoteDir,
            final String[] expectedDirsSrcFiles) throws Exception {
        final BPTransfer transfer = new BPTransfer("**/*", remoteDir, "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(remoteDir)).andReturn(false);
        expect(mockClient.makeDirectory(remoteDir)).andReturn(false);
        for (String subDir : expectedDirsRemoteDir) {
            expect(mockClient.changeDirectory(subDir)).andReturn(false);
            expect(mockClient.makeDirectory(subDir)).andReturn(true);
            expect(mockClient.changeDirectory(subDir)).andReturn(true);
        }
        final String srcPath = StringUtils.join(expectedDirsSrcFiles, '/');
        expect(mockClient.changeDirectory(srcPath)).andReturn(false);
        expect(mockClient.makeDirectory(srcPath)).andReturn(false);
        for (String subDir : expectedDirsSrcFiles) {
            expect(mockClient.changeDirectory(subDir)).andReturn(false);
            expect(mockClient.makeDirectory(subDir)).andReturn(true);
            expect(mockClient.changeDirectory(subDir)).andReturn(true);
        }
        expectTransferFile(transfer, srcFile);
        assertTransfer(transfer, 1);
    }

    @Test public void testCreateSingleDirectoryFromRemoteDirectoryAbsolute() throws Exception {
        final RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        final String dir = "newDir";
        final BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), "/" + dir, "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(dir)).andReturn(false);
        expect(mockClient.makeDirectory(dir)).andReturn(true);
        expect(mockClient.changeDirectory(dir)).andReturn(true);
        expectTransferFile(transfer, toTransfer);
        assertTransfer(transfer, 1);
    }

    @Test public void testCreateSingleDirectoryFromRemoteDirectoryAbsoluteWin() throws Exception {
        final RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        final String dir = "newDir";
        final BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), "\\" + dir, "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(dir)).andReturn(false);
        expect(mockClient.makeDirectory(dir)).andReturn(true);
        expect(mockClient.changeDirectory(dir)).andReturn(true);
        expectTransferFile(transfer, toTransfer);
        assertTransfer(transfer, 1);
    }

    @Test public void testCreateMultipleFilesWithDirectories() throws Exception {
        final String srcPath1 = "bit/of/a/trek/to";
        final String srcPath2 = "file/somewhere";
        final RandomFile srcFile1 = new RandomFile(baseDir.getRoot(), srcPath1 + "/my.file");
        final RandomFile srcFile2 = new RandomFile(baseDir.getRoot(), srcPath2 + "/else.log");
        final String remoteDir = "remote/root";
        final BPTransfer transfer = new BPTransfer("**/*", remoteDir, "", false, false);

        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(remoteDir)).andReturn(false);
        expect(mockClient.makeDirectory(remoteDir)).andReturn(true);
        expect(mockClient.changeDirectory(remoteDir)).andReturn(true);

        expect(mockClient.changeDirectory(srcPath1)).andReturn(false);
        expect(mockClient.makeDirectory(srcPath1)).andReturn(true);
        expect(mockClient.changeDirectory(srcPath1)).andReturn(true);

        expectTransferFile(transfer, srcFile1);

        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(remoteDir)).andReturn(true);

        expect(mockClient.changeDirectory(srcPath2)).andReturn(false);
        expect(mockClient.makeDirectory(srcPath2)).andReturn(true);
        expect(mockClient.changeDirectory(srcPath2)).andReturn(true);

        expectTransferFile(transfer, srcFile2);

        assertTransfer(transfer, 2);
    }

    @Test public void testFlatten() throws Exception {
        final String srcPath1 = "bit/of/a/trek/to";
        final String srcPath2 = "file/somewhere";
        final RandomFile srcFile1 = new RandomFile(baseDir.getRoot(), srcPath1 + "/my.file");
        final RandomFile srcFile2 = new RandomFile(baseDir.getRoot(), srcPath2 + "/else.log");
        final String remoteDir = "remote/root";
        final BPTransfer transfer = new BPTransfer("**/*", remoteDir, "", false, true);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(remoteDir)).andReturn(false);
        expect(mockClient.makeDirectory(remoteDir)).andReturn(true);
        expect(mockClient.changeDirectory(remoteDir)).andReturn(true);

        expectTransferFile(transfer, srcFile1);
        expectTransferFile(transfer, srcFile2);

        assertTransfer(transfer, 2);
    }

    @Test(expected = BapPublisherException.class)
    public void testFlattenThrowsExceptionIfTwoFilesHaveSameName() throws Exception {
        final String srcPath1 = "bit/of/a/trek/to";
        final String srcPath2 = "file/somewhere";
        final String duplicateFileName = "my.file";
        final RandomFile srcFile1 = new RandomFile(baseDir.getRoot(), srcPath1 + "/" + duplicateFileName);
        new RandomFile(baseDir.getRoot(), srcPath2 + "/" + duplicateFileName);
        final String remoteDir = "remote/root";
        final BPTransfer transfer = new BPTransfer("**/*", remoteDir, "", false, true);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(remoteDir)).andReturn(true);

        expectTransferFile(transfer, srcFile1);
        replayAndTransfer(transfer);
    }

    @Test public void testRemovePrefix() throws Exception {
        final String prefix = "gonna/remove";
        final String srcPath1 = "but/not/this";
        final String srcPath2 = "not/this/one/neither";
        final RandomFile srcFile1 = new RandomFile(baseDir.getRoot(), prefix + "/" + srcPath1 + "/my.file");
        final RandomFile srcFile2 = new RandomFile(baseDir.getRoot(), prefix + "/" + srcPath2 + "/else.log");

        final BPTransfer transfer = new BPTransfer("**/*", "", prefix, false, false);

        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(srcPath1)).andReturn(true);
        expectTransferFile(transfer, srcFile1);

        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(srcPath2)).andReturn(true);
        expectTransferFile(transfer, srcFile2);

        assertTransfer(transfer, 2);
    }

    @Test public void testRemovePrefixTrailingSlash() throws Exception {
        final String prefix = "gonna/remove/";
        final String srcPath1 = "but/not/this";
        final String srcPath2 = "not/this/one/neither";
        final RandomFile srcFile1 = new RandomFile(baseDir.getRoot(), prefix + srcPath1 + "/my.file");
        final RandomFile srcFile2 = new RandomFile(baseDir.getRoot(), prefix + srcPath2 + "/else.log");

        final BPTransfer transfer = new BPTransfer("**/*", "", prefix, false, false);

        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(srcPath1)).andReturn(true);
        expectTransferFile(transfer, srcFile1);

        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(srcPath2)).andReturn(true);
        expectTransferFile(transfer, srcFile2);

        assertTransfer(transfer, 2);
    }

    @Test public void testRemovePrefixPrecedingSlash() throws Exception {
        final String prefix = "\\gonna\\remove\\";
        final String expected = "but/not/this";
        final RandomFile srcFile = new RandomFile(baseDir.getRoot(), "gonna/remove/but/not/this/my.file");

        final BPTransfer transfer = new BPTransfer("**\\*", "", prefix, false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(expected)).andReturn(true);
        expectTransferFile(transfer, srcFile);

        assertTransfer(transfer, 1);
    }

    @Test public void testRemovePrefixThrowsExceptionIfPathDoesNotHavePrefix() throws Exception {
        final String prefix = "gonna/remove";
        final String srcPath = "but/not/this";
        new RandomFile(baseDir.getRoot(), srcPath + "/my.file");

        final BPTransfer transfer = new BPTransfer("**/*", "", prefix, false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);

        try {
            replayAndTransfer(transfer);
            fail();
        } catch (BapPublisherException bpe) {
            assertTrue(bpe.getMessage().contains(prefix));
        }
    }

    @Test public void testRemovePrefixCanUseEnvVars() throws Exception {
        envVars.put("BUILD_NUMBER", "123");
        final String prefix = "dir/$BUILD_NUMBER/hello";
        final String srcPath = "dir/123/hello/world";
        final RandomFile srcFile = new RandomFile(baseDir.getRoot(), srcPath + "/my.file");
        final BPTransfer transfer = new BPTransfer("**/*", "", prefix, false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory("world")).andReturn(true);
        expectTransferFile(transfer, srcFile);

        assertTransfer(transfer, 1);
    }

    @Test public void testRemoteDirectoryCanBeSimpleDateFormat() throws Exception {
        final RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        buildInfo.setBuildTime(createCalendar("25/11/2010 13:14:15"));
        final String dir = "'/myBuild-'yyyyMMdd-HHmmss";
        final String expected = "myBuild-20101125-131415";
        final BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), dir, "", true, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(expected)).andReturn(true);
        expectTransferFile(transfer, toTransfer);

        assertTransfer(transfer, 1);
    }

    @Test public void testRemoteDirectoryCanBeSimpleDateFormatAndUseEnvVars() throws Exception {
        final RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        buildInfo.setBuildTime(createCalendar("25/11/2010 13:14:15"));
        envVars.put("NODE_NAME", "slave1");
        final String dir = "'${NODE_NAME}-'yyyyMMdd";
        final String expected = "slave1-20101125";
        final BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), dir, "", true, false);

        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(expected)).andReturn(true);
        expectTransferFile(transfer, toTransfer);

        assertTransfer(transfer, 1);
    }

    @Test public void testExceptionIfBadSDFInRemoteDirectory() throws Exception {
        final RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        buildInfo.setBuildTime(createCalendar("25/11/2010 13:14:15"));
        final String dir = "/myBuild-yyyyMMdd-HHmmss";
        final BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), dir, "", true, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        try {
            replayAndTransfer(transfer);
            fail();
        } catch (BapPublisherException bpe) {
            assertTrue(bpe.getLocalizedMessage().contains(dir));
        }
    }

    @Test public void testPotentiallyHelpfulMessageIfBaseDirNotExist() throws Exception {
        buildInfo.setBaseDirectory(buildInfo.getBaseDirectory().child("IamNotThere"));
        final BPTransfer transfer = new BPTransfer("", "", "", true, false);
        try {
            replayAndTransfer(transfer);
            fail();
        } catch (BapPublisherException bpe) {
            assertTrue(bpe.getLocalizedMessage().contains(Messages.exception_baseDirectoryNotExist()));
        }
    }

    @Test public void testCanCompleteTransferIfCalledWithState() throws Exception {
        final RandomFile log1 = new RandomFile(baseDir.getRoot(), "1.log");
        final RandomFile log2 = new RandomFile(baseDir.getRoot(), "2.log");
        final RandomFile log3 = new RandomFile(baseDir.getRoot(), "3.log");
        final BPTransfer transfer = new BPTransfer("*", "", "", "", false, false, true);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        mockClient.deleteTree();
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expectTransferFile(transfer, log1, log2);
        expectLastCall().andThrow(new IOException());
        mockControl.replay();
        BPTransfer.TransferState state = null;
        try {
            transfer.transfer(buildInfo, mockClient);
            fail();
        } catch (BapTransferException bte) {
            state = bte.getState();
            assertNotNull(state);
        }
        mockControl.verify();
        mockControl.reset();
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expectTransferFile(transfer, log2, log3);
        final int expectedFileCount = 3;
        mockControl.replay();
        assertEquals(expectedFileCount, transfer.transfer(buildInfo, mockClient, state));
        mockControl.verify();
    }

    @Test public void testWillCleanAgainIfCleanFailedAndThenComplete() throws Exception {
        final RandomFile log1 = new RandomFile(baseDir.getRoot(), "1.log");
        final RandomFile log2 = new RandomFile(baseDir.getRoot(), "2.log");
        final RandomFile log3 = new RandomFile(baseDir.getRoot(), "3.log");
        final BPTransfer transfer = new BPTransfer("*", "", "", "", false, false, true);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        mockClient.deleteTree();
        expectLastCall().andThrow(new IOException());
        mockControl.replay();
        BPTransfer.TransferState state = null;
        try {
            transfer.transfer(buildInfo, mockClient);
            fail();
        } catch (BapTransferException bte) {
            state = bte.getState();
            assertNotNull(state);
        }
        mockControl.verify();
        mockControl.reset();
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        mockClient.deleteTree();
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expectTransferFile(transfer, log1, log2, log3);
        final int expectedFileCount = 3;
        mockControl.replay();
        assertEquals(expectedFileCount, transfer.transfer(buildInfo, mockClient, state));
        mockControl.verify();
    }

    private Calendar createCalendar(final String dateString) throws ParseException {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).parse(dateString));
        return calendar;
    }

    private void replayAndTransfer(final BPTransfer transfer) throws Exception {
        mockControl.replay();
        transfer.transfer(buildInfo, mockClient);
    }

    private void assertTransfer(final BPTransfer transfer, final int expectedFileCount) throws Exception {
        mockControl.replay();
        assertEquals(expectedFileCount, transfer.transfer(buildInfo, mockClient));
        mockControl.verify();
    }

    public void expectTransferFile(final BPTransfer transfer, final RandomFile... randomFiles) throws Exception {
       for (RandomFile randomFile : randomFiles) {
           mockClient.transferFile(same(transfer), eq(new FilePath(randomFile.getFile())), streamContains(randomFile.getContents()));
       }
   }


}
