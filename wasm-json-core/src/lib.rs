use serde_json::Value;
use rmp_serde::to_vec;
use std::mem;

#[no_mangle]
pub extern "C" fn allocate(size: usize) -> *mut u8 {
    let mut buf = Vec::with_capacity(size);
    let ptr = buf.as_mut_ptr();
    mem::forget(buf);
    ptr
}

#[no_mangle]
pub extern "C" fn deallocate(ptr: *mut u8, size: usize) {
    unsafe {
        let _ = Vec::from_raw_parts(ptr, size, size);
    }
}

#[no_mangle]
pub extern "C" fn parse_to_msgpack(ptr: *mut u8, len: usize) -> *const u8 {
    let json_slice = unsafe { std::slice::from_raw_parts(ptr, len) };
    
    let parsed: Value = match serde_json::from_slice(json_slice) {
        Ok(v) => v,
        Err(_) => return -1_i32 as *const u8,
    };
    
    // Serialize to MessagePack
    let mut msgpack = match to_vec(&parsed) {
        Ok(v) => v,
        Err(_) => to_vec(&Value::Null).unwrap(),
    };
    
    // Prefix with 4 bytes of length (Little Endian)
    let len = msgpack.len() as u32;
    let len_bytes = len.to_le_bytes();
    
    let mut result = Vec::with_capacity(4 + msgpack.len());
    result.extend_from_slice(&len_bytes);
    result.append(&mut msgpack);
    
    let ptr = result.as_ptr();
    mem::forget(result); // Java must deallocate this!
    ptr
}
