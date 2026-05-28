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

fn value_to_msgpack(val: &serde_json::Value, buf: &mut Vec<u8>) {
    match val {
        serde_json::Value::Null => rmp::encode::write_nil(buf).unwrap(),
        serde_json::Value::Bool(b) => rmp::encode::write_bool(buf, *b).unwrap(),
        serde_json::Value::Number(n) => {
            if let Some(i) = n.as_i64() {
                rmp::encode::write_i64(buf, i).unwrap();
            } else if let Some(u) = n.as_u64() {
                rmp::encode::write_u64(buf, u).unwrap();
            } else {
                // For everything else (including floats and big numbers),
                // use the string representation to ensure precision.
                rmp::encode::write_str(buf, &n.to_string()).unwrap();
            }
        }
        serde_json::Value::String(s) => rmp::encode::write_str(buf, s).unwrap(),
        serde_json::Value::Array(a) => {
            rmp::encode::write_array_len(buf, a.len() as u32).unwrap();
            for v in a { value_to_msgpack(v, buf); }
        }
        serde_json::Value::Object(o) => {
            rmp::encode::write_map_len(buf, o.len() as u32).unwrap();
            for (k, v) in o {
                rmp::encode::write_str(buf, k).unwrap();
                value_to_msgpack(v, buf);
            }
        }
    }
}

#[no_mangle]
pub extern "C" fn parse_to_msgpack(ptr: *mut u8, len: usize) -> *const u8 {
    let json_slice = unsafe { std::slice::from_raw_parts(ptr, len) };
    
    let parsed: serde_json::Value = match serde_json::from_slice(json_slice) {
        Ok(v) => v,
        Err(_) => return -1_i32 as *const u8,
    };
    
    let mut msgpack = Vec::new();
    value_to_msgpack(&parsed, &mut msgpack);
    
    let len = msgpack.len() as u32;
    let len_bytes = len.to_le_bytes();
    
    let mut result = Vec::with_capacity(4 + msgpack.len());
    result.extend_from_slice(&len_bytes);
    result.append(&mut msgpack);
    
    let ptr = result.as_ptr();
    mem::forget(result); 
    ptr
}

#[cfg(test)]
mod tests {
    #[test]
    fn test_invalid_json() {
        let json = b"[1 true]";
        let res: Result<serde_json::Value, _> = serde_json::from_slice(json);
        println!("Result is: {:?}", res);
        assert!(res.is_err(), "Expected error, got {:?}", res);
    }
}

