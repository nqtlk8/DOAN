async function test() {
  try {
    const login = await fetch('http://localhost:8080/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'password' })
    });
    const loginData = await login.json();
    const token = loginData.data.accessToken;
    
    const formData = {
      name: 'Nhà phân phối',
      phone: '',
      email: '',
      address: '',
      taxCode: '',
      region: 'TPHCM'
    };
    
    const payload = { ...formData, code: formData.code || `SUP-${Date.now()}` };

    const res = await fetch('http://localhost:8080/api/v1/suppliers', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify(payload)
    });
    
    console.log("Status:", res.status);
    console.log("Response:", await res.json());
  } catch (e) {
    console.error("Error:", e);
  }
}

test();
