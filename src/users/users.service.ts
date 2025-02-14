import { Injectable } from '@nestjs/common';
import { Repository } from 'typeorm';
import { InjectRepository } from '@nestjs/typeorm';
import { User } from './user.entity';

@Injectable()
export class UsersService {
    constructor(@InjectRepository(User) private repo:Repository<User>){}

    create(email:string,password:string){
        const user= this.repo.create({email: email, password:password})

        return this.repo.save(user)
    }
    findAll(): Promise<User[]>{
        return this.repo.find();

    }
    findOne(id:number): Promise<User>{
        return this.repo.findOneBy({id})
    }
    async updateOne(id:number,userData: any): Promise<User | string>{
        const user = await this.repo.findOneBy({id}) || null
        console.log(user)
        if (user){
            this.repo.merge(user, userData);
            return this.repo.save(user)
        }
        return "Not Found"
    }
}
