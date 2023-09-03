import { Component } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { Dialog } from "primereact/dialog";
import _ from "lodash";
import axios from "axios";
import Home from "./components/Home";
import Execute from "./components/Execute";
import Error404 from "./components/Error404";
import './App.css';

class App extends Component {

    constructor(props) {
        super(props);
        this.state = {
            userState: null,
            socket: null,
            socketId: null,
            lastErrorMessage: null,
            lastExecution: null,
            setLastExecution: lastExecution => this.setState({ lastExecution }),
            lastDeployment: null,
            ready: false
        };
        this.login = this.login.bind(this);
        this.logout = this.logout.bind(this);
        this.register = this.register.bind(this);
        this.execute = this.execute.bind(this);
        this.deploy = this.deploy.bind(this);
    }

    login(username, password) {
        if (!this.state.userState) {
            this.state.socket.send(JSON.stringify({ type: "login", username, password }));
            sessionStorage.setItem("username", username);
            sessionStorage.setItem("password", password);
        }
    }

    logout() {
        if (this.state.userState) {
            this.state.socket.send(JSON.stringify({ type: "logout" }));
            this.setState({ userState: null });
            sessionStorage.removeItem("username");
            sessionStorage.removeItem("password");
        }
    }

    register(username, password) {
        if (!this.state.userState) {
            this.state.socket.send(JSON.stringify({ type: "register", username, password }));
            this.setState({ username });
        }
    }

    execute(id, args) {
        if (this.state.userState) {
            this.state.socket.send(JSON.stringify({ type: "execute", id, args: _.join(args, ";") }));
        }
    }

    deploy(name, file) {
        if (this.state.userState) {
            this.setState({ lastDeployment: name });
            axios.postForm(
                "http://localhost:8081/service/deploy",
                {
                    name,
                    file,
                    id: this.state.socketId
                }
            );
        }
    }

    componentDidMount() {
        const socket = new WebSocket("ws://localhost:8081/service/ws");
        socket.onmessage = e => {
            const message = JSON.parse(e.data);
            switch (message.type) {
                case "sendId":
                    const username = sessionStorage.getItem("username");
                    const password = sessionStorage.getItem("password");
                    if (username && password) {
                        this.state.socket.send(JSON.stringify({ type: "login", username, password }));
                        this.setState({ socketId: message.id });
                    } else {
                        this.setState({ socketId: message.id, ready: true });
                    }
                    break;
                case "loginOutput":
                    if (message.error) {
                        this.setState({ lastErrorMessage: message.error });
                    }
                    if (message.exec) {
                        this.setState({ userState: { executables: message.exec }, ready: true });
                    }
                    break;
                case "deployOutput":
                    if (message.error) {
                        this.setState({ lastErrorMessage: message.error, lastDeployment: null });
                    }
                    if (message.id) {
                        const userState = {
                            ...this.state.userState,
                            executables: [
                                ...this.state.userState.executables,
                                { id: message.id, name: this.state.lastDeployment }
                            ]
                        };
                        sessionStorage.setItem("userState", JSON.stringify(userState));
                        this.setState({
                            userState,
                            lastDeployment: null
                        });
                    }
                    break;
                case "executeOutput":
                    if (message.error) {
                        this.setState({ lastErrorMessage: message.error });
                    }
                    if (message.output) {
                        this.setState({ lastExecution: message.output });
                    }
                    break;
                default:
            }
        };
        this.setState({ socket });
    }

    componentWillUnmount() {
        if (this.state.socket !== null) {
            this.state.socket.close();
        }
    }

    render() {
        if (!this.state.ready) {
            return null;
        }
        return (
            <>
                <Dialog
                    dismissableMask={ true }
                    draggable={ false }
                    resizable={ false }
                    header={ <h3>An error has occurred</h3> }
                    visible={ this.state.lastErrorMessage !== null }
                    onHide={ () => this.setState({ lastErrorMessage: null }) }
                >
                    <p>{ this.state.lastErrorMessage }</p>
                </Dialog>
                <Routes>
                    <Route
                        path="/"
                        element={
                            this.state.userState === null
                            ? <Home login={ this.login } register={ this.register } />
                            : <Navigate to="/execute" />
                        }
                    />
                    <Route
                        path="/execute"
                        element={
                            this.state.userState === null
                            ? <Navigate to="/" />
                            : <Execute
                                userState={ this.state.userState }
                                logout={ this.logout }
                                deploy={ this.deploy }
                                execute={ this.execute }
                                lastExecution={ this.state.lastExecution }
                                setLastExecution={ this.state.setLastExecution }
                                lastDeployment={ this.state.lastDeployment }
                              />
                        }
                    />
                    <Route path="*" element={ <Error404 /> } />
                </Routes>
            </>
        );
    }
}

export default App;
