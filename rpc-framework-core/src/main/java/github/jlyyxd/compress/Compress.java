package github.jlyyxd.compress;

import github.jlxxyd.extension.SPI;

@SPI
public interface Compress {

    byte[] compress(byte[] bytes);

    byte[] decompress(byte[] bytes);
}
