package dev.lukebemish.codecextras.compat.nightconfig;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.mojang.serialization.DynamicOps;
import dev.lukebemish.codecextras.config.OpsIo;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TomlConfigOpsIo implements OpsIo<Object> {
	public static final TomlConfigOpsIo INSTANCE = new TomlConfigOpsIo();

	@Override
	public DynamicOps<Object> ops() {
		return TomlConfigOps.COMMENTED;
	}

	private static final TomlParser PARSER = TomlFormat.instance().createParser().setLenientWithBareKeys(true).setLenientWithSeparators(true);
	private static final TomlWriter WRITER = TomlFormat.instance().createWriter();

	@Override
	public Object read(InputStream input) throws IOException {
		try {
			return PARSER.parse(input);
		} catch (ParsingException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void write(Object value, OutputStream output) throws IOException {
		try {
			if (value instanceof CommentedConfig config) {
				WRITER.write(config, output);
			} else {
				throw new IOException("Cannot write non-config object");
			}
		} catch (WritingException e) {
			throw new IOException(e);
		}
	}
}
