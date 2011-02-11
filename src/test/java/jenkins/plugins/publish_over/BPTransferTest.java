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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

import static jenkins.plugins.publish_over.helper.InputStreamMatcher.streamContains;
import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class BPTransferTest {
    
    @Rule public TemporaryFolder baseDir = new TemporaryFolder();
    protected Map<String, String> envVars = new TreeMap<String, String>();
    protected BPBuildInfo buildInfo;
    protected IMocksControl mockControl = EasyMock.createStrictControl();
    protected BPClient mockClient = mockControl.createMock(BPClient.class);
    
    @Before
    public void setUp() throws Exception {
        BPBuildEnv current = new BPBuildInfoFactory().createEmptyBuildEnv();
        buildInfo = new BPBuildInfo(TaskListener.NULL, "", new FilePath(new File("")), current, null);
        buildInfo.setEnvVars(envVars);
        buildInfo.setBaseDirectory(new FilePath(baseDir.getRoot()));
        buildInfo.setBuildTime(Calendar.getInstance());
    }

    @Test public void testSingleFileInRootWithExplicitPath() throws Exception {
        testSingleFileInRoot("xxx.log", "xxx.log");
    }

    @Test public void testSingleFileInRootWithExtensionGlob() throws Exception {
        testSingleFileInRoot("xxx.log", "xxx.*");
    }

    @Test public void testSingleFileNoExtensionInRootWithMatchAnyFile() throws Exception {
        testSingleFileInRoot("xxx", "*");
    }

    @Test public void testSingleFileInRootWithMatchAnyFile() throws Exception {
        testSingleFileInRoot("xxx.log", "*");
    }

    @Test public void testSingleFileInRootWithMatchAnyFileWithAnyExtension() throws Exception {
        testSingleFileInRoot("xxx.log", "*.*");
    }

    @Test public void testSingleFileInRootWithMatchAnyFileWithAnyPathAndExtension() throws Exception {
        testSingleFileInRoot("xxx.log", "**/*.*");
    }

    @Test public void testSingleFileInRootWithMatchAnyFileWithAnyPathAndFile() throws Exception {
        testSingleFileInRoot("xxx.log", "**/*");
    }
    
    @Test public void testSingleFileInRootWithMatchAnyFileWithAnyPathAndFileWin() throws Exception {
        testSingleFileInRoot("xxx.log", "**\\*");
    }

    @Test public void testSingleFileNoExtensionInRootWithMatchAnyFileWithAnyPathAndFile() throws Exception {
        testSingleFileInRoot("xxx", "**/*");
    }

    private void testSingleFileInRoot(String filename, String pattern) throws Exception {
        RandomFile toTransfer = new RandomFile(baseDir.getRoot(), filename);
        BPTransfer transfer = new BPTransfer(pattern, "", "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expectTransferFile(transfer, toTransfer);
        testTransfer(transfer, 1);
    }
    
    @Test public void testExceptionPropagatesWhenFailToTransferFile() throws Exception {
        RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "abc.jpg");
        BPTransfer transfer = new BPTransfer("*", "", "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        Exception someException = new Exception("meh");
        mockClient.transferFile(EasyMock.same(transfer), EasyMock.eq(new FilePath(toTransfer.getFile())), streamContains(toTransfer.getContents()));
        expectLastCall().andThrow(someException);
        try {
            testTransfer(transfer, 99);
            fail();
        } catch (Exception e) {
            assertSame(someException, e);
        }
    }

    @Test public void testMultipleFiles() throws Exception {
        RandomFile log1 = new RandomFile(baseDir.getRoot(), "one.log");
        RandomFile log2 = new RandomFile(baseDir.getRoot(), "two.log");
        RandomFile log3 = new RandomFile(baseDir.getRoot(), "three.log");
        new RandomFile(baseDir.getRoot(), "notALog.txt");
        BPTransfer transfer = new BPTransfer("*.log", "", "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        mockControl.checkOrder(false);
        expectTransferFile(transfer, log1, log2, log3);
        mockControl.checkOrder(true);
        testTransfer(transfer, 3);
    }

    @Test public void testEnvVarInPattern() throws Exception {
        RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello_123.txt");
        BPTransfer transfer = new BPTransfer("hello_${BUILD_NUMBER}.*", "", "", false, false);
        envVars.put("BUILD_NUMBER", "123");
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expectTransferFile(transfer, toTransfer);
        testTransfer(transfer, 1);
    }

    @Test public void testCreateSingleDirectoryFromRemoteDirectory() throws Exception {
        RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        String dir = "newDir";
        BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), dir, "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(dir)).andReturn(false);
        expect(mockClient.makeDirectory(dir)).andReturn(true);
        expect(mockClient.changeDirectory(dir)).andReturn(true);
        expectTransferFile(transfer, toTransfer);
        testTransfer(transfer, 1);
    }

    @Test public void testCreateSingleDirectoryFromRemoteDirectoryWithEnvVar() throws Exception {
        RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        envVars.put("BUILD_NUMBER", "123");
        String expandedDir = "newDir-123";
        BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), "newDir-${BUILD_NUMBER}", "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(expandedDir)).andReturn(false);
        expect(mockClient.makeDirectory(expandedDir)).andReturn(true);
        expect(mockClient.changeDirectory(expandedDir)).andReturn(true);
        expectTransferFile(transfer, toTransfer);
        testTransfer(transfer, 1);
    }

    @Test public void testCreateMultipleDirectoriesFromRemoteDirectory() throws Exception {
        testCreateMultipleDirectoriesFromRemoteDirectory("newDir/and/another", new String[] {"newDir", "and", "another"});
    }

    @Test public void testCreateMultipleDirectoriesFromRemoteDirectoryWin() throws Exception {
        testCreateMultipleDirectoriesFromRemoteDirectory("newDir\\and\\another", new String[] {"newDir", "and", "another"});
    }

    @Test public void testCreateMultipleDirectoriesFromRemoteDirectoryTrailingSeparator() throws Exception {
        testCreateMultipleDirectoriesFromRemoteDirectory("newDir/and/another/", new String[] {"newDir", "and", "another"});
    }

    @Test public void testCreateMultipleDirectoriesFromRemoteDirectoryTrailingSeparatorWin() throws Exception {
        testCreateMultipleDirectoriesFromRemoteDirectory("newDir\\and\\another\\", new String[] {"newDir", "and", "another"});
    }

    private void testCreateMultipleDirectoriesFromRemoteDirectory(String remoteDir, String[] expectedDirs) throws Exception {
        String normalizedDir = remoteDir.contains("\\") ? remoteDir.replaceAll("\\\\", "/") : remoteDir;
        RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), remoteDir, "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(normalizedDir)).andReturn(false);
        expect(mockClient.makeDirectory(normalizedDir)).andReturn(false);
        for (String subDir : expectedDirs) {
            expect(mockClient.changeDirectory(subDir)).andReturn(false);
            expect(mockClient.makeDirectory(subDir)).andReturn(true);
            expect(mockClient.changeDirectory(subDir)).andReturn(true);
        }
        expectTransferFile(transfer, toTransfer);
        testTransfer(transfer, 1);
    }
    

    @Test public void testCreateDirectoriesFromSrcFileAndRemoteDirectory() throws Exception {
        RandomFile srcFile = new RandomFile(baseDir.getRoot(), "bit/of/a/trek/to/my.file");
        testCreateDirectories(srcFile, "my/remote/dir", new String[] {"my", "remote", "dir"},
            new String[] {"bit", "of", "a", "trek", "to"});
    }

    private void testCreateDirectories(RandomFile srcFile, String remoteDir, String[] expectedDirsRemoteDir,
            String[] expectedDirsSrcFiles) throws Exception {
        BPTransfer transfer = new BPTransfer("**/*", remoteDir, "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(remoteDir)).andReturn(false);
        expect(mockClient.makeDirectory(remoteDir)).andReturn(false);
        for (String subDir : expectedDirsRemoteDir) {
            expect(mockClient.changeDirectory(subDir)).andReturn(false);
            expect(mockClient.makeDirectory(subDir)).andReturn(true);
            expect(mockClient.changeDirectory(subDir)).andReturn(true);
        }
        String srcPath = StringUtils.join(expectedDirsSrcFiles, '/');
        expect(mockClient.changeDirectory(srcPath)).andReturn(false);
        expect(mockClient.makeDirectory(srcPath)).andReturn(false);
        for (String subDir : expectedDirsSrcFiles) {
            expect(mockClient.changeDirectory(subDir)).andReturn(false);
            expect(mockClient.makeDirectory(subDir)).andReturn(true);
            expect(mockClient.changeDirectory(subDir)).andReturn(true);
        }
        expectTransferFile(transfer, srcFile);
        testTransfer(transfer, 1);
    }

    @Test public void testCreateSingleDirectoryFromRemoteDirectoryAbsolute() throws Exception {
        RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        String dir = "newDir";
        BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), "/" + dir, "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(dir)).andReturn(false);
        expect(mockClient.makeDirectory(dir)).andReturn(true);
        expect(mockClient.changeDirectory(dir)).andReturn(true);
        expectTransferFile(transfer, toTransfer);
        testTransfer(transfer, 1);
    }
    
    @Test public void testCreateSingleDirectoryFromRemoteDirectoryAbsoluteWin() throws Exception {
        RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        String dir = "newDir";
        BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), "\\" + dir, "", false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(dir)).andReturn(false);
        expect(mockClient.makeDirectory(dir)).andReturn(true);
        expect(mockClient.changeDirectory(dir)).andReturn(true);
        expectTransferFile(transfer, toTransfer);
        testTransfer(transfer, 1);
    }
    
    @Test public void testCreateMultipleFilesWithDirectories() throws Exception {
        String srcPath1 = "bit/of/a/trek/to";
        String srcPath2 = "file/somewhere";
        RandomFile srcFile1 = new RandomFile(baseDir.getRoot(), srcPath1 + "/my.file");
        RandomFile srcFile2 = new RandomFile(baseDir.getRoot(), srcPath2 + "/else.log");
        String remoteDir = "remote/root";
        BPTransfer transfer = new BPTransfer("**/*", remoteDir, "", false, false);
        
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

        testTransfer(transfer, 2);
    }

    @Test public void testFlatten() throws Exception {
        String srcPath1 = "bit/of/a/trek/to";
        String srcPath2 = "file/somewhere";
        RandomFile srcFile1 = new RandomFile(baseDir.getRoot(), srcPath1 + "/my.file");
        RandomFile srcFile2 = new RandomFile(baseDir.getRoot(), srcPath2 + "/else.log");
        String remoteDir = "remote/root";
        BPTransfer transfer = new BPTransfer("**/*", remoteDir, "", false, true);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(remoteDir)).andReturn(false);
        expect(mockClient.makeDirectory(remoteDir)).andReturn(true);
        expect(mockClient.changeDirectory(remoteDir)).andReturn(true);

        expectTransferFile(transfer, srcFile1);
        expectTransferFile(transfer, srcFile2);

        testTransfer(transfer, 2);
    }

    @Test(expected = BapPublisherException.class)
    public void testFlattenThrowsExceptionIfTwoFilesHaveSameName() throws Exception {
        String srcPath1 = "bit/of/a/trek/to";
        String srcPath2 = "file/somewhere";
        RandomFile srcFile1 = new RandomFile(baseDir.getRoot(), srcPath1 + "/my.file");
        RandomFile srcFile2 = new RandomFile(baseDir.getRoot(), srcPath2 + "/my.file");
        String remoteDir = "remote/root";
        BPTransfer transfer = new BPTransfer("**/*", remoteDir, "", false, true);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(remoteDir)).andReturn(true);
        
        
        expectTransferFile(transfer, srcFile1);
        testTransfer(transfer, 99);
    }

    @Test public void testRemovePrefix() throws Exception {
        String prefix = "gonna/remove";
        String srcPath1 = "but/not/this";
        String srcPath2 = "not/this/one/neither";
        RandomFile srcFile1 = new RandomFile(baseDir.getRoot(), prefix + "/" + srcPath1 + "/my.file");
        RandomFile srcFile2 = new RandomFile(baseDir.getRoot(), prefix + "/" + srcPath2 + "/else.log");

        BPTransfer transfer = new BPTransfer("**/*", "", prefix, false, false);
        
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(srcPath1)).andReturn(true);
        expectTransferFile(transfer, srcFile1);

        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(srcPath2)).andReturn(true);
        expectTransferFile(transfer, srcFile2);

        testTransfer(transfer, 2);
    }

    @Test public void testRemovePrefixTrailingSlash() throws Exception {
        String prefix = "gonna/remove/";
        String srcPath1 = "but/not/this";
        String srcPath2 = "not/this/one/neither";
        RandomFile srcFile1 = new RandomFile(baseDir.getRoot(), prefix + srcPath1 + "/my.file");
        RandomFile srcFile2 = new RandomFile(baseDir.getRoot(), prefix + srcPath2 + "/else.log");

        BPTransfer transfer = new BPTransfer("**/*", "", prefix, false, false);
        
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(srcPath1)).andReturn(true);
        expectTransferFile(transfer, srcFile1);

        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(srcPath2)).andReturn(true);
        expectTransferFile(transfer, srcFile2);

        testTransfer(transfer, 2);
    }

    @Test public void testRemovePrefixPrecedingSlash() throws Exception {
        String prefix = "\\gonna\\remove\\";
        String expected = "but/not/this";
        RandomFile srcFile = new RandomFile(baseDir.getRoot(), "gonna/remove/but/not/this/my.file");

        BPTransfer transfer = new BPTransfer("**\\*", "", prefix, false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(expected)).andReturn(true);
        expectTransferFile(transfer, srcFile);

        testTransfer(transfer, 1);
    }

    @Test public void testRemovePrefixThrowsExceptionIfPathDoesNotHavePrefix() throws Exception {
        String prefix = "gonna/remove";
        String srcPath = "but/not/this";
        new RandomFile(baseDir.getRoot(), srcPath + "/my.file");

        BPTransfer transfer = new BPTransfer("**/*", "", prefix, false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);

        try {
            testTransfer(transfer, 99);
            fail();
        } catch (BapPublisherException bpe) {
            assertTrue(bpe.getMessage().contains(prefix));
        }
    }

    @Test public void testRemovePrefixCanUseEnvVars() throws Exception {
        envVars.put("BUILD_NUMBER", "123");
        String prefix = "dir/$BUILD_NUMBER/hello";
        String srcPath = "dir/123/hello/world";
        RandomFile srcFile = new RandomFile(baseDir.getRoot(), srcPath + "/my.file");
        BPTransfer transfer = new BPTransfer("**/*", "", prefix, false, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory("world")).andReturn(true);
        expectTransferFile(transfer, srcFile);

        testTransfer(transfer, 1);
    }

    @Test public void testRemoteDirectoryCanBeSimpleDateFormat() throws Exception {
        RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        Calendar buildTime = Calendar.getInstance();
        buildTime.setTime(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse("25/11/2010 13:14:15"));
        buildInfo.setBuildTime(buildTime);
        String dir = "'/myBuild-'yyyyMMdd-HHmmss";
        String expected = "myBuild-20101125-131415";
        BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), dir, "", true, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(expected)).andReturn(true);
        expectTransferFile(transfer, toTransfer);

        testTransfer(transfer, 1);
    }

    @Test public void testRemoteDirectoryCanBeSimpleDateFormatAndUseEnvVars() throws Exception {
        RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        Calendar buildTime = Calendar.getInstance();
        buildTime.setTime(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse("25/11/2010 13:14:15"));
        buildInfo.setBuildTime(buildTime);
        envVars.put("NODE_NAME", "slave1");
        String dir = "'${NODE_NAME}-'yyyyMMdd";
        String expected = "slave1-20101125";
        BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), dir, "", true, false);
        
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        expect(mockClient.changeDirectory(expected)).andReturn(true);
        expectTransferFile(transfer, toTransfer);

        testTransfer(transfer, 1);
    }

    @Test public void testExceptionIfBadSDFInRemoteDirectory() throws Exception {
        RandomFile toTransfer = new RandomFile(baseDir.getRoot(), "hello.txt");
        Calendar buildTime = Calendar.getInstance();
        buildTime.setTime(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse("25/11/2010 13:14:15"));
        buildInfo.setBuildTime(buildTime);
        String dir = "/myBuild-yyyyMMdd-HHmmss";
        BPTransfer transfer = new BPTransfer(toTransfer.getFileName(), dir, "", true, false);
        expect(mockClient.changeToInitialDirectory()).andReturn(true);
        try {
            testTransfer(transfer, 99);
            fail();
        } catch (BapPublisherException bpe) {
            assertTrue(bpe.getLocalizedMessage().contains(dir));
        }
    }

    private void testTransfer(BPTransfer transfer, int expectedFileCount) throws Exception {
        mockControl.replay();
        assertEquals(expectedFileCount, transfer.transfer(buildInfo, mockClient));
        mockControl.verify();
    }
    
    public void expectTransferFile(BPTransfer transfer, RandomFile... randomFiles) throws Exception {
       for (RandomFile randomFile : randomFiles) {
           mockClient.transferFile(same(transfer), eq(new FilePath(randomFile.getFile())), streamContains(randomFile.getContents()));
       }
   }
    
    
}
