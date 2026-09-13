use chacha20::cipher::{KeyIvInit, StreamCipher};
use chacha20::ChaCha20;
use hex;
use jni::objects::{JByteArray, JClass};
use jni::sys::jbyteArray;
use jni::JNIEnv;

use super::keys::key;

// You need to create a key.rs at the same level as this file and add the following code:

// use obfstr::obfstr;

//  ChaCha20 encryption key (hex-encoded), obfuscated at compile time
// #[inline(always)]
// pub fn key() -> String {
//     obfstr!("758xxxxx Your Own ChaCha20 Key").to_string()
// }

// Decode the hex-encoded ChaCha20 key at runtime
#[inline(always)]
fn get_runtime_key() -> [u8; 32] {
    let decoded = hex::decode(key()).expect("Invalid hex in KEY");
    let mut key = [0u8; 32];
    key.copy_from_slice(&decoded);
    key
}

#[allow(non_snake_case)]
#[cold]
pub fn decryptJar(env: JNIEnv, _class: JClass, data: JByteArray) -> jbyteArray {
    let data_bytes = env
        .convert_byte_array(&data)
        .expect("Couldn't convert byte array");
    if data_bytes.len() <= 12 {
        return std::ptr::null_mut();
    }

    let nonce = &data_bytes[0..12];
    let ciphertext = &data_bytes[12..];

    let key = get_runtime_key();
    let mut cipher = ChaCha20::new(&key.into(), nonce.into());

    let mut plaintext = ciphertext.to_vec();
    cipher.apply_keystream(&mut plaintext);

    let result = env
        .byte_array_from_slice(&plaintext)
        .expect("Couldn't create byte array");
    result.into_raw()
}
