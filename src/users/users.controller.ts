import { Post,Controller, Body,Response,Get } from '@nestjs/common';
import { createUserDto } from './dtos/create-user.dto';
import { UsersService } from './users.service';

@Controller('auth')
export class UsersController {
    constructor(private userService: UsersService){}

    @Post('/signup')
    createUser(@Body() body:createUserDto){
        this.userService.create(body.email,body.password)
        return "User created"
    }
    @Get('/users')
    getUsers(){
        return this.userService.findAll()
    }
}
