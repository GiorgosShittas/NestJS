import { IsString,IsEmail } from "class-validator"
export class createUserDto{
    @IsEmail()
    email: string;

    @IsString()
    password: string;
}