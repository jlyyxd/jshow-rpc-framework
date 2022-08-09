package github.jlyyxd.compress;

import github.jlyyxd.extension.SPI;

@SPI
public interface Compress {

    byte[] compress(byte[] bytes);

    byte[] decompress(byte[] bytes);
}
