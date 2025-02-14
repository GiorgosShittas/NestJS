import { Post,Controller, Body,Response,Get,Param,Put} from '@nestjs/common';
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
    @Get("/users/:id")
    getUser(@Param('id') id : number){
        return this.userService.findOne(id)
    }
    @Put('/users/:id')
    updateUser(@Param('id') id: number,@Body() body:any){
        return this.userService.updateOne(id,body)
    }
}
