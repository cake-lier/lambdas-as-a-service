import { InputText } from "primereact/inputtext";
import { Button } from "primereact/button";
import { Password } from "primereact/password";
import { useCallback, useState } from "react";

export default function LoginForm({ setUser, isLogin }) {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const doLogin = useCallback(e => {
        e.preventDefault();
        setUser(email, password);
    }, [email, password, setUser]);
    return (
        <div className="grid">
            <div className="col-12 flex justify-content-center">
                <img className="h-5rem" src="logo512.png" alt="App logo" />
            </div>
            <div className="col-12 mt-5">
                <h1 className="font-bold text-4xl">{ isLogin ? "Log in" : "Sign up" }</h1>
            </div>
            <div className="col-12 mt-3">
                <form onSubmit={ doLogin } >
                    <span className="p-float-label p-input-icon-right w-full">
                        <i className="pi pi-envelope" />
                        <InputText
                            id="email"
                            name="email"
                            className="w-full"
                            value={ email }
                            onChange={ e => { if (e.target.value.length <= 40) { setEmail(e.target.value) } } }
                        />
                        <label htmlFor="email">E-mail</label>
                    </span>
                    <span className="p-float-label mt-4 w-full">
                        <Password
                            id="password"
                            name="password"
                            className="w-full"
                            value={ password }
                            onChange={ e => setPassword(e.target.value) }
                            feedback={ false }
                            toggleMask
                        />
                        <label htmlFor="password">Password</label>
                    </span>
                    <Button
                        type="submit"
                        className="w-full mt-5"
                        label={ isLogin ? "Login" : "Sign up" }
                    />
                </form>
            </div>
        </div>
    );
}