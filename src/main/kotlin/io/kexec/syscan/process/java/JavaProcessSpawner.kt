package io.kexec.syscan.process.java

import com.zaxxer.nuprocess.NuAbstractProcessHandler
import com.zaxxer.nuprocess.NuProcess
import com.zaxxer.nuprocess.NuProcessBuilder
import io.kexec.syscan.io.FsPath
import io.kexec.syscan.io.java.toJavaPath
import io.kexec.syscan.process.ProcessResult
import io.kexec.syscan.process.ProcessSpawner
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

object JavaProcessSpawner : ProcessSpawner {
  override fun execute(executable: String, args: List<String>, workingDirectory: FsPath?): ProcessResult {
    val builder = NuProcessBuilder(listOf(executable, *args.toTypedArray()))
    if (workingDirectory != null) {
      builder.setCwd(workingDirectory.toJavaPath())
    }
    val handler = BufferedProcessHandler()
    builder.setProcessListener(handler)
    val process = builder.start()
    val exitCode = process.waitFor(0, TimeUnit.SECONDS)
    return handler.toProcessResult(exitCode)
  }

  class BufferedProcessHandler : NuAbstractProcessHandler() {
    private val stdoutByteStream = ByteArrayOutputStream()
    private val stderrByteStream = ByteArrayOutputStream()

    lateinit var process: NuProcess

    override fun onStart(nuProcess: NuProcess) {
      process = nuProcess
    }

    override fun onStdinReady(buffer: ByteBuffer): Boolean {
      process.closeStdin(true)
      return false
    }

    override fun onStdout(buffer: ByteBuffer, closed: Boolean) {
      val remaining = buffer.remaining()
      val bytes = ByteArray(remaining)
      buffer.get(bytes)
      stdoutByteStream.writeBytes(bytes)
    }

    override fun onStderr(buffer: ByteBuffer, closed: Boolean) {
      val remaining = buffer.remaining()
      val bytes = ByteArray(remaining)
      buffer.get(bytes)
      stdoutByteStream.writeBytes(bytes)
    }

    fun toProcessResult(exitCode: Int): ProcessResult = ProcessResult(
      exitCode,
      stdoutByteStream.toByteArray(),
      stderrByteStream.toByteArray()
    )
  }
}
