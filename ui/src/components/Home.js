import { Card } from 'primereact/card';
import LoginForm from "./LoginForm";

export default function Home({ login, register }) {
    return (
        <div className="grid h-screen align-items-center">
            <div className="col-6 md:col-4 md:col-offset-1">
                <Card>
                    <LoginForm
                        setUser={ register }
                        isLogin={ false }
                    />
                </Card>
            </div>
            <div className="col-6 md:col-4 md:col-offset-2">
                <Card>
                    <LoginForm
                        setUser={ login }
                        isLogin={ true }
                    />
                </Card>
            </div>
        </div>
    );
}
